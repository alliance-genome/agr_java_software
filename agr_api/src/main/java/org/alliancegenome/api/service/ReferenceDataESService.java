package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.api.es.dao.SearchDAO;
import org.alliancegenome.api.es.query.Pagination;
import org.alliancegenome.api.response.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.es.util.EsClientFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.ParsedCardinality;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.collapse.CollapseBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

import jakarta.enterprise.context.RequestScoped;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class ReferenceDataESService extends ESService {

	private static final List<String> MODEL_SUBJECT_CATEGORIES = List.of(
		"agm_disease_annotation",
		"agm_phenotype_annotation"
	);

	private static final SearchDAO SEARCH_DAO = new SearchDAO();

	// ES defaults `index.max_terms_count` to 65_536; keep our exclusion list under that.
	private static final int MAX_TRANSGENIC_EXCLUSION_TERMS = 65000;

	private static final int MAX_MODEL_BUCKETS = 1000;

	// Genes cited by a paper are attached to gene_summary docs via the
	// `referenceCuries` array (populated during curation). Query gene_summary
	// directly by that field rather than aggregating annotation subjects.
	@SuppressWarnings("unchecked")
	public JsonResultResponse<Map<String, Object>> getGenesByReference(String referenceCurie, Pagination pagination) {
		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", "gene_summary"))
			.filter(termQuery("referenceCuries.keyword", referenceCurie));

		SearchResponse resp = SEARCH_DAO.performQuery(
			query, List.of(), null,
			List.of("gene"),
			pagination.getLimit(), pagination.getStart(),
			new HighlightBuilder(), null, false);

		List<Map<String, Object>> genes = new ArrayList<>();
		for (SearchHit hit : resp.getHits().getHits()) {
			Object geneObj = hit.getSourceAsMap().get("gene");
			if (geneObj instanceof Map) {
				genes.add((Map<String, Object>) geneObj);
			}
		}
		JsonResultResponse<Map<String, Object>> ret = new JsonResultResponse<>();
		ret.setTotal((int) resp.getHits().getTotalHits().value);
		ret.setResults(genes);
		return ret;
	}

	// Alleles cited by a paper live on allele_summary docs via `allele.references[].curie`.
	// To keep the Alleles/Variants section mutually exclusive from Transgenic Alleles, we
	// pull the transgenic primaryExternalIds for this reference and exclude them. If the
	// paper has more transgenics than we can fit in a single ES `terms` clause, we log and
	// skip the exclusion (returning the un-excluded set) so the page still renders.
	public JsonResultResponse<Map<String, Object>> getAllelesByReference(String referenceCurie, Pagination pagination) {
		List<String> transgenicIds = fetchTransgenicPrimaryExternalIds(referenceCurie);
		if (transgenicIds == null) {
			log.warn("Reference {} has more than {} transgenic alleles; alleles response will overlap with transgenic-alleles for this ref.",
				referenceCurie, MAX_TRANSGENIC_EXCLUSION_TERMS);
			transgenicIds = List.of();
		}
		return getAlleleCategoryByReference("allele_summary", referenceCurie, pagination, transgenicIds);
	}

	// Distinct transgenic-allele primaryExternalIds cited by `referenceCurie`, or null if
	// the count exceeds the exclusion cap (in which case the caller should skip exclusion).
	// Uses a terms aggregation so we get all IDs in one round-trip.
	private List<String> fetchTransgenicPrimaryExternalIds(String referenceCurie) {
		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", "transgenic_allele_summary"))
			.filter(termQuery("allele.references.curie.keyword", referenceCurie));

		SearchSourceBuilder src = new SearchSourceBuilder()
			.query(query)
			.size(0)
			.trackTotalHits(false)
			.aggregation(AggregationBuilders.terms("ids")
				.field("allele.primaryExternalId.keyword")
				.size(MAX_TRANSGENIC_EXCLUSION_TERMS + 1));

		SearchResponse resp;
		try {
			resp = EsClientFactory.getDefaultEsClient()
				.search(new SearchRequest(ConfigHelper.getEsIndex()).source(src), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new RuntimeException("Failed to enumerate transgenic ids for reference " + referenceCurie, e);
		}
		ParsedStringTerms terms = resp.getAggregations().get("ids");
		List<? extends Terms.Bucket> buckets = terms.getBuckets();
		if (buckets.size() > MAX_TRANSGENIC_EXCLUSION_TERMS || terms.getSumOfOtherDocCounts() > 0) {
			return null;
		}
		List<String> ids = new ArrayList<>(buckets.size());
		for (Terms.Bucket bucket : buckets) {
			ids.add(bucket.getKeyAsString());
		}
		return ids;
	}

	// Transgenic alleles are indexed on their own transgenic_allele_summary category with
	// the same `allele.references[].curie` shape. That category never carries
	// `allele.alleleSynonyms`, so enrich each row from the allele_summary sibling doc.
	@SuppressWarnings("unchecked")
	public JsonResultResponse<Map<String, Object>> getTransgenicAllelesByReference(String referenceCurie, Pagination pagination) {
		JsonResultResponse<Map<String, Object>> ret = getAlleleCategoryByReference("transgenic_allele_summary", referenceCurie, pagination, List.of());
		List<String> ids = new ArrayList<>();
		for (Map<String, Object> row : ret.getResults()) {
			Object idObj = ((Map<String, Object>) row.get("allele")).get("primaryExternalId");
			if (idObj != null) {
				ids.add(idObj.toString());
			}
		}
		if (ids.isEmpty()) {
			return ret;
		}
		Map<String, Object> synonymsById = fetchAlleleSynonyms(ids);
		for (Map<String, Object> row : ret.getResults()) {
			Map<String, Object> allele = (Map<String, Object>) row.get("allele");
			Object idObj = allele.get("primaryExternalId");
			if (idObj == null) {
				continue;
			}
			Object syn = synonymsById.get(idObj.toString());
			if (syn != null) {
				allele.put("alleleSynonyms", syn);
			}
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> fetchAlleleSynonyms(List<String> primaryExternalIds) {
		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", "allele_summary"))
			.filter(termsQuery("allele.primaryExternalId.keyword", primaryExternalIds));

		SearchSourceBuilder src = new SearchSourceBuilder()
			.query(query)
			.fetchSource(new String[]{"allele.primaryExternalId", "allele.alleleSynonyms"}, null)
			.size(primaryExternalIds.size())
			.from(0)
			.trackTotalHits(false);

		SearchResponse resp;
		try {
			resp = EsClientFactory.getDefaultEsClient()
				.search(new SearchRequest(ConfigHelper.getEsIndex()).source(src), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new RuntimeException("Failed to fetch allele_summary synonyms for transgenic enrichment", e);
		}
		Map<String, Object> byId = new LinkedHashMap<>();
		for (SearchHit hit : resp.getHits().getHits()) {
			Object alleleObj = hit.getSourceAsMap().get("allele");
			if (!(alleleObj instanceof Map)) {
				continue;
			}
			Map<String, Object> allele = (Map<String, Object>) alleleObj;
			Object idObj = allele.get("primaryExternalId");
			if (idObj == null) {
				continue;
			}
			Object syn = allele.get("alleleSynonyms");
			if (syn != null) {
				byId.put(idObj.toString(), syn);
			}
		}
		return byId;
	}

	// Both allele categories share the same `allele` shape; collapse defensively so that
	// any curation-side duplicate primaryExternalIds within a single category surface once.
	// Row shape carries the fields the reference page tables render: top-level `allele`,
	// `alterationType`, `variantList`, plus `transgenicAlleleConstructs` for the transgenic
	// category.
	@SuppressWarnings("unchecked")
	private JsonResultResponse<Map<String, Object>> getAlleleCategoryByReference(String category, String referenceCurie, Pagination pagination, List<String> excludePrimaryExternalIds) {
		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", category))
			.filter(termQuery("allele.references.curie.keyword", referenceCurie));
		if (!excludePrimaryExternalIds.isEmpty()) {
			query.mustNot(termsQuery("allele.primaryExternalId.keyword", excludePrimaryExternalIds));
		}

		SearchSourceBuilder src = new SearchSourceBuilder()
			.query(query)
			.fetchSource(new String[]{
				"allele", "alterationType", "variantList", "transgenicAlleleConstructs"
			}, null)
			.collapse(new CollapseBuilder("allele.primaryExternalId.keyword"))
			.size(pagination.getLimit())
			.from(pagination.getStart())
			.trackTotalHits(true)
			.aggregation(AggregationBuilders.cardinality("distinctAlleles")
				.field("allele.primaryExternalId.keyword"));

		SearchResponse resp;
		try {
			resp = EsClientFactory.getDefaultEsClient()
				.search(new SearchRequest(ConfigHelper.getEsIndex()).source(src), RequestOptions.DEFAULT);
		} catch (IOException e) {
			throw new RuntimeException("Failed to query " + category + " for reference " + referenceCurie, e);
		}

		List<Map<String, Object>> rows = new ArrayList<>();
		for (SearchHit hit : resp.getHits().getHits()) {
			Map<String, Object> source = hit.getSourceAsMap();
			if (!(source.get("allele") instanceof Map)) {
				continue;
			}
			rows.add(new LinkedHashMap<>(source));
		}
		ParsedCardinality distinct = resp.getAggregations().get("distinctAlleles");
		JsonResultResponse<Map<String, Object>> ret = new JsonResultResponse<>();
		ret.setTotal((int) distinct.getValue());
		ret.setResults(rows);
		return ret;
	}

	// Models cited by a paper: aggregate `subject.primaryExternalId` across the two
	// AGM-annotation categories and pull a single AGM sample per distinct model.
	// (`model_search_result` docs don't carry a references field yet — see
	// docs/ROADMAP for the indexer change that would let us query them directly.)
	public JsonResultResponse<Map<String, Object>> getModelsByReference(String referenceCurie) {
		BoolQueryBuilder query = boolQuery()
			.filter(termsQuery("category", MODEL_SUBJECT_CATEGORIES))
			.must(termQuery("references.curie.keyword", referenceCurie));

		AggregationBuilder agg = AggregationBuilders
			.terms("models")
			.field("subject.primaryExternalId.keyword")
			.size(MAX_MODEL_BUCKETS)
			.subAggregation(AggregationBuilders.topHits("sample")
				.size(1)
				.fetchSource(new String[]{"subject"}, null));

		SearchResponse resp = SEARCH_DAO.performQuery(
			query, List.of(agg), null, List.of(), 0, 0,
			new HighlightBuilder(), null, false);

		ParsedStringTerms terms = resp.getAggregations().get("models");
		if (terms.getSumOfOtherDocCounts() > 0) {
			log.warn("Reference {} has more than {} distinct AGMs; {} doc(s) fell out of the terms aggregation.",
				referenceCurie, MAX_MODEL_BUCKETS, terms.getSumOfOtherDocCounts());
		}
		List<Map<String, Object>> rows = new ArrayList<>();
		for (Terms.Bucket bucket : terms.getBuckets()) {
			Map<String, Object> row = buildModelRow(bucket);
			if (row != null) {
				rows.add(row);
			}
		}
		JsonResultResponse<Map<String, Object>> ret = new JsonResultResponse<>();
		ret.setTotal(rows.size());
		ret.setResults(rows);
		return ret;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> buildModelRow(Terms.Bucket bucket) {
		TopHits topHits = bucket.getAggregations().get("sample");
		SearchHit[] hits = topHits.getHits().getHits();
		if (hits.length == 0) {
			return null;
		}
		Map<String, Object> agm = (Map<String, Object>) hits[0].getSourceAsMap().get("subject");
		if (agm == null) {
			return null;
		}
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("model", agm);
		String source = extractAgmSource(agm);
		if (source != null) {
			row.put("dataProvider", source);
		}
		return row;
	}

	@SuppressWarnings("unchecked")
	private String extractAgmSource(Map<String, Object> agm) {
		Object dpc = agm.get("dataProviderCrossReference");
		if (!(dpc instanceof Map)) {
			return null;
		}
		Object rdp = ((Map<String, Object>) dpc).get("resourceDescriptorPage");
		if (!(rdp instanceof Map)) {
			return null;
		}
		Object rd = ((Map<String, Object>) rdp).get("resourceDescriptor");
		if (!(rd instanceof Map)) {
			return null;
		}
		Object name = ((Map<String, Object>) rd).get("name");
		return name == null ? null : name.toString();
	}
}
