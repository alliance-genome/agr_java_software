package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.api.es.dao.SearchDAO;
import org.alliancegenome.api.es.query.Pagination;
import org.alliancegenome.api.response.JsonResultResponse;
import org.alliancegenome.api.service.helper.APIServiceHelper;
import org.alliancegenome.core.document.AGMDiseaseAnnotationDocument;
import org.alliancegenome.core.document.AlleleDiseaseAnnotationDocument;
import org.alliancegenome.core.document.AllelePhenotypeAnnotationDocument;
import org.alliancegenome.core.document.DiseaseAnnotationDocument;
import org.alliancegenome.core.document.GeneDiseaseAnnotationDocument;
import org.alliancegenome.core.document.GeneGeneticInteractionDocument;
import org.alliancegenome.core.document.GeneMolecularInteractionDocument;
import org.alliancegenome.core.document.GenePhenotypeAnnotationDocument;
import org.alliancegenome.core.document.LiteratureSummaryDocument;
import org.alliancegenome.core.document.PhenotypeAnnotationDocument;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.sort.SortOrder;

import jakarta.enterprise.context.RequestScoped;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class ReferenceDataESService extends ESService {

	private static final List<String> DISEASE_CATEGORIES = List.of(
		"gene_disease_annotation",
		"allele_disease_annotation",
		"agm_disease_annotation"
	);

	private static final List<String> PHENOTYPE_CATEGORIES = List.of(
		"gene_phenotype_annotation",
		"allele_phenotype_annotation",
		"agm_phenotype_annotation"
	);

	private static final List<String> GENE_SUBJECT_CATEGORIES = List.of(
		"gene_disease_annotation",
		"gene_phenotype_annotation"
	);

	private static final List<String> ALLELE_SUBJECT_CATEGORIES = List.of(
		"allele_disease_annotation",
		"allele_phenotype_annotation"
	);

	private static final List<String> MODEL_SUBJECT_CATEGORIES = List.of(
		"agm_disease_annotation",
		"agm_phenotype_annotation"
	);

	private static final SearchDAO SEARCH_DAO = new SearchDAO();

	private static final int MAX_DEDUPE_HITS = 10000;

	private static final List<String> DEDUPE_SOURCE_FIELDS = List.of(
		"primaryAnnotations.inferredGene.primaryExternalId",
		"primaryAnnotations.inferredAllele.primaryExternalId",
		"phenotypeStatement",
		"object.curie"
	);

	private static final int MAX_MODEL_BUCKETS = 1000;

	private static final int MAX_MODEL_SAMPLES_PER_AGM = 100;

	private static final int MAX_AGM_PHENOTYPE_HITS = 10000;

	// Stage indexes the same annotation at gene/allele/agm levels; dedupe in Java by
	// (inferredGene, inferredAllele, secondary) and drop the gene-level rollup when an
	// allele-level row already covers the same (gene, secondary) pair.
	public JsonResultResponse<DiseaseAnnotationDocument> getDiseaseAnnotations(String referenceCurie, Pagination pagination) {
		BoolQueryBuilder query = boolQuery()
			.filter(termsQuery("category", DISEASE_CATEGORIES))
			.must(termQuery("references.curie.keyword", referenceCurie));
		addTableFilter(pagination, query);

		List<SearchHit> allHits = fetchDedupeHits(query);
		List<SearchHit> deduped = dedupeByGeneAlleleAndSecondary(allHits, "disease");
		List<SearchHit> page = sliceForPagination(deduped, pagination);

		JsonResultResponse<DiseaseAnnotationDocument> ret = new JsonResultResponse<>();
		ret.setTotal(deduped.size());
		List<DiseaseAnnotationDocument> results = new ArrayList<>();
		for (SearchHit hit : rehydratePage(page)) {
			DiseaseAnnotationDocument doc = deserializeDiseaseByCategory(hit);
			if (doc != null) {
				results.add(doc);
			}
		}
		ret.setResults(results);
		return ret;
	}

	public JsonResultResponse<PhenotypeAnnotationDocument> getPhenotypeAnnotations(String referenceCurie, Pagination pagination) {
		BoolQueryBuilder query = boolQuery()
			.filter(termsQuery("category", PHENOTYPE_CATEGORIES))
			.must(termQuery("references.curie.keyword", referenceCurie));
		addTableFilter(pagination, query);

		List<SearchHit> allHits = fetchDedupeHits(query);
		List<SearchHit> deduped = dedupeByGeneAlleleAndSecondary(allHits, "phenotype");
		List<SearchHit> page = sliceForPagination(deduped, pagination);

		JsonResultResponse<PhenotypeAnnotationDocument> ret = new JsonResultResponse<>();
		ret.setTotal(deduped.size());
		List<PhenotypeAnnotationDocument> results = new ArrayList<>();
		for (SearchHit hit : rehydratePage(page)) {
			PhenotypeAnnotationDocument doc = deserializePhenotypeByCategory(hit);
			if (doc != null) {
				results.add(doc);
			}
		}
		ret.setResults(results);
		return ret;
	}

	// Lightweight pass that only pulls the dedupe-key fields; the visible page is then
	// re-hydrated with full _source via rehydratePage. This keeps per-hit payload tiny
	// for the dedupe sweep and avoids deserializing rows that won't render.
	// On papers where the underlying hit count exceeds MAX_DEDUPE_HITS, we log and
	// continue with the truncated set rather than failing the request so page 1 still
	// renders (the count is then a best-effort lower bound).
	private List<SearchHit> fetchDedupeHits(BoolQueryBuilder query) {
		SearchResponse resp = SEARCH_DAO.performQuery(
			(QueryBuilder) query, List.of(), null, DEDUPE_SOURCE_FIELDS,
			MAX_DEDUPE_HITS, 0,
			new org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder(), null, false);
		long total = resp.getHits().getTotalHits().value;
		if (total > MAX_DEDUPE_HITS) {
			log.warn("Reference annotation hit count {} exceeds dedupe limit {}; truncating dedupe input and returning best-effort total.",
				total, MAX_DEDUPE_HITS);
		}
		return new ArrayList<>(Arrays.asList(resp.getHits().getHits()));
	}

	private List<SearchHit> rehydratePage(List<SearchHit> pageHits) {
		if (pageHits.isEmpty()) {
			return List.of();
		}
		String[] ids = new String[pageHits.size()];
		for (int i = 0; i < pageHits.size(); i++) {
			ids[i] = pageHits.get(i).getId();
		}
		BoolQueryBuilder byIds = boolQuery().must(QueryBuilders.idsQuery().addIds(ids));
		SearchResponse resp = SEARCH_DAO.performQuery(
			(QueryBuilder) byIds, List.of(), null, List.of(),
			ids.length, 0,
			new org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder(), null, false);
		Map<String, SearchHit> byId = new HashMap<>();
		for (SearchHit hit : resp.getHits().getHits()) {
			byId.put(hit.getId(), hit);
		}
		List<SearchHit> ordered = new ArrayList<>(pageHits.size());
		for (SearchHit hit : pageHits) {
			SearchHit full = byId.get(hit.getId());
			if (full != null) {
				ordered.add(full);
			}
		}
		return ordered;
	}

	private List<SearchHit> sliceForPagination(List<SearchHit> hits, Pagination pagination) {
		int from = Math.max(0, Math.min(pagination.getOffset(), hits.size()));
		int to = Math.min(from + pagination.getLimit(), hits.size());
		return hits.subList(from, to);
	}

	private List<SearchHit> dedupeByGeneAlleleAndSecondary(List<SearchHit> hits, String secondaryKind) {
		Map<String, SearchHit> seen = new LinkedHashMap<>();
		Map<SearchHit, List<String[]>> keysByHit = new LinkedHashMap<>();
		for (SearchHit hit : hits) {
			List<String[]> keys = extractDedupeKeys(hit.getSourceAsMap(), secondaryKind);
			boolean firstClaim = false;
			for (String[] key : keys) {
				String composite = key[0] + "|" + key[1] + "|" + key[2];
				if (seen.putIfAbsent(composite, hit) == null) {
					firstClaim = true;
				}
			}
			if (firstClaim) {
				keysByHit.put(hit, keys);
			}
		}

		Set<String> hasAlleleForGeneSecondary = new HashSet<>();
		for (List<String[]> keys : keysByHit.values()) {
			for (String[] key : keys) {
				if (!key[1].isEmpty()) {
					hasAlleleForGeneSecondary.add(key[0] + "|" + key[2]);
				}
			}
		}

		List<SearchHit> out = new ArrayList<>();
		for (Map.Entry<SearchHit, List<String[]>> entry : keysByHit.entrySet()) {
			boolean hasAlleleKey = false;
			for (String[] key : entry.getValue()) {
				if (!key[1].isEmpty()) {
					hasAlleleKey = true;
					break;
				}
			}
			if (hasAlleleKey) {
				out.add(entry.getKey());
				continue;
			}
			boolean shadowedByAlleleSibling = false;
			for (String[] key : entry.getValue()) {
				if (hasAlleleForGeneSecondary.contains(key[0] + "|" + key[2])) {
					shadowedByAlleleSibling = true;
					break;
				}
			}
			if (!shadowedByAlleleSibling) {
				out.add(entry.getKey());
			}
		}
		return out;
	}

	// One key per distinct (inferredGene, inferredAllele) pair in primaryAnnotations.
	// Earlier this used only primaryAnnotations[0], which silently dropped sibling
	// allele claims when a single doc rolled up multiple alleles.
	@SuppressWarnings("unchecked")
	private List<String[]> extractDedupeKeys(Map<String, Object> src, String secondaryKind) {
		String secondary = "";
		if ("phenotype".equals(secondaryKind)) {
			Object stmt = src.get("phenotypeStatement");
			if (stmt != null) {
				secondary = stmt.toString();
			}
		} else if ("disease".equals(secondaryKind)) {
			Object obj = src.get("object");
			if (obj instanceof Map) {
				Object curie = ((Map<String, Object>) obj).get("curie");
				if (curie != null) {
					secondary = curie.toString();
				}
			}
		}

		List<String[]> keys = new ArrayList<>();
		Object paObj = src.get("primaryAnnotations");
		if (paObj instanceof List) {
			Set<String> seenComposite = new HashSet<>();
			for (Object item : (List<Object>) paObj) {
				if (!(item instanceof Map)) {
					continue;
				}
				Map<String, Object> pa = (Map<String, Object>) item;
				String geneId = nestedId(pa, "inferredGene");
				String alleleId = nestedId(pa, "inferredAllele");
				if (seenComposite.add(geneId + "|" + alleleId)) {
					keys.add(new String[]{geneId, alleleId, secondary});
				}
			}
		}
		if (keys.isEmpty()) {
			keys.add(new String[]{"", "", secondary});
		}
		return keys;
	}

	@SuppressWarnings("unchecked")
	private String nestedId(Map<String, Object> parent, String field) {
		Object v = parent.get(field);
		if (!(v instanceof Map)) {
			return "";
		}
		Object id = ((Map<String, Object>) v).get("primaryExternalId");
		return id == null ? "" : id.toString();
	}

	// Expression docs index publications as `referenceXrefs` (PMID/MOD CrossReference objects).
	// The caller passes the set of cross-reference curies (PMID/MOD) from the literature summary.
	public JsonResultResponse<GeneExpressionDocument> getExpressionAnnotations(List<String> crossReferenceCuries, Pagination pagination) {
		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", "gene_expression_annotation"));
		if (crossReferenceCuries != null && !crossReferenceCuries.isEmpty()) {
			query.must(termsQuery("referenceXrefs.referencedCurie.keyword", crossReferenceCuries));
		}
		return runTypedQuery(query, pagination, GeneExpressionDocument.class);
	}

	// Molecular interaction docs only index `geneMolecularInteraction.evidence.referenceID` (PMID/MOD IDs).
	public JsonResultResponse<GeneMolecularInteractionDocument> getMolecularInteractions(List<String> crossReferenceCuries, Pagination pagination) {
		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", "gene_molecular_interaction"));
		if (crossReferenceCuries != null && !crossReferenceCuries.isEmpty()) {
			query.must(termsQuery("geneMolecularInteraction.evidence.referenceID.keyword", crossReferenceCuries));
		}
		return runTypedQuery(query, pagination, GeneMolecularInteractionDocument.class);
	}

	// Genetic interaction category mirrors molecular interaction shape. Note: stage ES currently has
	// zero docs in `gene_genetic_interaction` (indexing gap); code works once the category is populated.
	public JsonResultResponse<GeneGeneticInteractionDocument> getGeneticInteractions(List<String> crossReferenceCuries, Pagination pagination) {
		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", "gene_genetic_interaction"));
		if (crossReferenceCuries != null && !crossReferenceCuries.isEmpty()) {
			query.must(termsQuery("geneGeneticInteraction.evidence.referenceID.keyword", crossReferenceCuries));
		}
		return runTypedQuery(query, pagination, GeneGeneticInteractionDocument.class);
	}

	private <T> JsonResultResponse<T> runTypedQuery(BoolQueryBuilder query, Pagination pagination, Class<T> type) {
		addTableFilter(pagination, query);
		SearchResponse searchResponse = getSearchResponse(query, pagination, null, false);

		JsonResultResponse<T> ret = new JsonResultResponse<>();
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		List<T> results = new ArrayList<>();
		for (SearchHit hit : searchResponse.getHits().getHits()) {
			T doc = mapHit(hit, type);
			if (doc != null) {
				results.add(doc);
			}
		}
		ret.setResults(results);
		return ret;
	}

	// The embedded `subject` gene on annotation docs is a thin projection that omits
	// geneType (biotype) and geneGenomicLocationAssociations. The full gene doc lives
	// in the gene_summary category; enrich each reference-scoped gene with that doc.
	public JsonResultResponse<Map<String, Object>> getGenesByReference(String referenceCurie) {
		JsonResultResponse<Map<String, Object>> baseResp = getDistinctSubjects(GENE_SUBJECT_CATEGORIES, referenceCurie);
		List<String> geneIds = new ArrayList<>();
		for (Map<String, Object> g : baseResp.getResults()) {
			Object id = g.get("primaryExternalId");
			if (id != null) {
				geneIds.add(id.toString());
			}
		}
		Map<String, Map<String, Object>> fullGenes = lookupGeneSummaries(geneIds);

		List<Map<String, Object>> enriched = new ArrayList<>();
		for (Map<String, Object> g : baseResp.getResults()) {
			Object id = g.get("primaryExternalId");
			Map<String, Object> full = id == null ? null : fullGenes.get(id.toString());
			enriched.add(full != null ? full : g);
		}
		JsonResultResponse<Map<String, Object>> ret = new JsonResultResponse<>();
		ret.setTotal(enriched.size());
		ret.setResults(enriched);
		return ret;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Map<String, Object>> lookupGeneSummaries(List<String> geneIds) {
		if (geneIds.isEmpty()) {
			return Map.of();
		}
		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", "gene_summary"))
			.filter(termsQuery("gene.primaryExternalId.keyword", geneIds));

		SearchResponse resp = SEARCH_DAO.performQuery(
			(QueryBuilder) query, List.of(), null,
			List.of("gene"),
			geneIds.size() * 2, 0,
			new org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder(), null, false);

		Map<String, Map<String, Object>> out = new LinkedHashMap<>();
		for (SearchHit hit : resp.getHits().getHits()) {
			Map<String, Object> src = hit.getSourceAsMap();
			Object geneObj = src.get("gene");
			if (!(geneObj instanceof Map)) {
				continue;
			}
			Map<String, Object> gene = (Map<String, Object>) geneObj;
			Object id = gene.get("primaryExternalId");
			if (id != null) {
				out.put(id.toString(), gene);
			}
		}
		return out;
	}

	public JsonResultResponse<Map<String, Object>> getAllelesByReference(String referenceCurie) {
		return getDistinctSubjects(ALLELE_SUBJECT_CATEGORIES, referenceCurie);
	}

	public JsonResultResponse<Map<String, Object>> getModelsByReference(String referenceCurie) {
		Map<String, Map<String, Object>> rowsByAgm = aggregateModelsWithDiseases(referenceCurie);
		Map<String, Set<String>> phenotypesByAgm = aggregatePhenotypesByAgm(referenceCurie);
		for (Map.Entry<String, Map<String, Object>> entry : rowsByAgm.entrySet()) {
			Set<String> phenotypes = phenotypesByAgm.get(entry.getKey());
			if (phenotypes != null) {
				entry.getValue().put("associatedPhenotype", new ArrayList<>(phenotypes));
			}
		}
		JsonResultResponse<Map<String, Object>> ret = new JsonResultResponse<>();
		List<Map<String, Object>> models = new ArrayList<>(rowsByAgm.values());
		ret.setTotal(models.size());
		ret.setResults(models);
		return ret;
	}

	private Map<String, Map<String, Object>> aggregateModelsWithDiseases(String referenceCurie) {
		BoolQueryBuilder query = boolQuery()
			.filter(termsQuery("category", MODEL_SUBJECT_CATEGORIES))
			.must(termQuery("references.curie.keyword", referenceCurie));

		AggregationBuilder agg = AggregationBuilders
			.terms("models")
			.field("subject.primaryExternalId.keyword")
			.size(MAX_MODEL_BUCKETS)
			.subAggregation(AggregationBuilders.topHits("samples")
				.size(MAX_MODEL_SAMPLES_PER_AGM)
				.fetchSource(new String[]{"subject", "object", "generatedRelationString", "phenotypeStatement", "category"}, null));

		SearchResponse searchResponse = SEARCH_DAO.performQuery(
			(QueryBuilder) query, List.of(agg), null, List.of(), 0, 0,
			new org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder(), null, false);

		Map<String, Map<String, Object>> rowsByAgm = new LinkedHashMap<>();
		ParsedStringTerms terms = searchResponse.getAggregations().get("models");
		long otherAgmDocCount = terms.getSumOfOtherDocCounts();
		if (otherAgmDocCount > 0) {
			log.warn("Reference {} has more than {} distinct AGMs; {} doc(s) fell out of the terms aggregation.",
				referenceCurie, MAX_MODEL_BUCKETS, otherAgmDocCount);
		}
		for (Terms.Bucket bucket : terms.getBuckets()) {
			Map<String, Object> row = buildModelRow(bucket, referenceCurie);
			if (row != null) {
				rowsByAgm.put(bucket.getKeyAsString(), row);
			}
		}
		return rowsByAgm;
	}

	private Map<String, Object> buildModelRow(Terms.Bucket bucket, String referenceCurie) {
		TopHits topHits = bucket.getAggregations().get("samples");
		long bucketTotal = topHits.getHits().getTotalHits().value;
		if (bucketTotal > MAX_MODEL_SAMPLES_PER_AGM) {
			log.warn("Reference {} AGM {} has {} sample hits; truncated to {} for disease/phenotype rollup.",
				referenceCurie, bucket.getKeyAsString(), bucketTotal, MAX_MODEL_SAMPLES_PER_AGM);
		}
		Map<String, Object> agm = null;
		Map<String, Map<String, Object>> diseases = new LinkedHashMap<>();
		Set<String> phenotypes = new LinkedHashSet<>();

		for (SearchHit hit : topHits.getHits().getHits()) {
			Map<String, Object> src = hit.getSourceAsMap();
			if (agm == null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> subject = (Map<String, Object>) src.get("subject");
				agm = subject;
			}
			String category = (String) src.get("category");
			if ("agm_disease_annotation".equals(category)) {
				@SuppressWarnings("unchecked")
				Map<String, Object> obj = (Map<String, Object>) src.get("object");
				if (obj != null) {
					String diseaseCurie = (String) obj.get("curie");
					if (diseaseCurie != null && !diseases.containsKey(diseaseCurie)) {
						Map<String, Object> diseaseModel = new LinkedHashMap<>();
						diseaseModel.put("disease", obj);
						diseaseModel.put("associationType", src.get("generatedRelationString"));
						diseases.put(diseaseCurie, diseaseModel);
					}
				}
			} else if ("agm_phenotype_annotation".equals(category)) {
				Object stmt = src.get("phenotypeStatement");
				if (stmt instanceof String) {
					phenotypes.add((String) stmt);
				}
			}
		}

		if (agm == null) {
			return null;
		}
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("model", agm);
		row.put("diseaseModels", new ArrayList<>(diseases.values()));
		row.put("associatedPhenotype", new ArrayList<>(phenotypes));
		String source = extractAgmSource(agm);
		if (source != null) {
			row.put("dataProvider", source);
		}
		return row;
	}

	// AGM phenotypes for this paper may live on allele/gene phenotype rows whose
	// primaryAnnotations[].phenotypeAnnotationSubject is an AffectedGenomicModel.
	// Fetch all phenotype-category docs scoped by reference and bucket the phenotype
	// statements by the AGM they refer to.
	@SuppressWarnings("unchecked")
	private Map<String, Set<String>> aggregatePhenotypesByAgm(String referenceCurie) {
		BoolQueryBuilder query = boolQuery()
			.filter(termsQuery("category", PHENOTYPE_CATEGORIES))
			.must(termQuery("references.curie.keyword", referenceCurie));

		SearchResponse resp = SEARCH_DAO.performQuery(
			(QueryBuilder) query, List.of(), null,
			List.of("phenotypeStatement", "primaryAnnotations"),
			MAX_AGM_PHENOTYPE_HITS, 0,
			new org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder(), null, false);

		long total = resp.getHits().getTotalHits().value;
		if (total > MAX_AGM_PHENOTYPE_HITS) {
			log.warn("Reference {} has {} phenotype docs for AGM rollup; truncating to {}.",
				referenceCurie, total, MAX_AGM_PHENOTYPE_HITS);
		}
		Map<String, Set<String>> out = new LinkedHashMap<>();
		for (SearchHit hit : resp.getHits().getHits()) {
			Map<String, Object> src = hit.getSourceAsMap();
			Object stmtObj = src.get("phenotypeStatement");
			if (!(stmtObj instanceof String)) {
				continue;
			}
			String stmt = (String) stmtObj;
			Object paObj = src.get("primaryAnnotations");
			if (!(paObj instanceof List)) {
				continue;
			}
			for (Object item : (List<Object>) paObj) {
				if (!(item instanceof Map)) {
					continue;
				}
				Map<String, Object> pa = (Map<String, Object>) item;
				Object subjObj = pa.get("phenotypeAnnotationSubject");
				if (!(subjObj instanceof Map)) {
					continue;
				}
				Map<String, Object> subj = (Map<String, Object>) subjObj;
				if (!"AffectedGenomicModel".equals(subj.get("type"))) {
					continue;
				}
				Object agmId = subj.get("primaryExternalId");
				if (agmId == null) {
					continue;
				}
				out.computeIfAbsent(agmId.toString(), k -> new LinkedHashSet<>()).add(stmt);
			}
		}
		return out;
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

	private JsonResultResponse<Map<String, Object>> getDistinctSubjects(List<String> categories, String referenceCurie) {
		BoolQueryBuilder query = boolQuery()
			.filter(termsQuery("category", categories))
			.must(termQuery("references.curie.keyword", referenceCurie));

		AggregationBuilder agg = AggregationBuilders
			.terms("subjects")
			.field("subject.primaryExternalId.keyword")
			.size(1000)
			.subAggregation(AggregationBuilders.topHits("sample").size(1)
				.fetchSource(new String[]{"subject"}, null));

		SearchResponse searchResponse = SEARCH_DAO.performQuery(
			(QueryBuilder) query, List.of(agg), null, List.of("subject"), 0, 0,
			new org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder(), null, false);

		JsonResultResponse<Map<String, Object>> ret = new JsonResultResponse<>();
		List<Map<String, Object>> subjects = new ArrayList<>();

		ParsedStringTerms terms = searchResponse.getAggregations().get("subjects");
		for (Terms.Bucket bucket : terms.getBuckets()) {
			TopHits topHits = bucket.getAggregations().get("sample");
			SearchHit[] hits = topHits.getHits().getHits();
			if (hits.length > 0) {
				@SuppressWarnings("unchecked")
				Map<String, Object> src = (Map<String, Object>) hits[0].getSourceAsMap().get("subject");
				if (src != null) {
					subjects.add(src);
				}
			}
		}

		ret.setTotal(subjects.size());
		ret.setResults(subjects);
		return ret;
	}

	// literatureSummary.title/abstract/mods_in_corpus are indexed as of the Mapping change; match the disease in title
	// OR abstract, bucket by MOD, and keep the `latest` most-recently-published papers per MOD via a top_hits sub-agg.
	public JsonResultResponse<LiteratureSummaryDocument> getLatestLiteratureByDiseasePerMod(String disease, int latest) {
		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", "literature_summary"))
			.should(matchQuery("literatureSummary.title", disease))
			.should(matchQuery("literatureSummary.abstract", disease))
			.minimumShouldMatch(1);

		AggregationBuilder agg = AggregationBuilders
			.terms("by_mod")
			.field("literatureSummary.mods_in_corpus")
			.size(30)
			.subAggregation(AggregationBuilders.topHits("latest").size(latest)
				.sort("literatureSummary.date_published.keyword", SortOrder.DESC));

		SearchResponse searchResponse = SEARCH_DAO.performQuery(
			(QueryBuilder) query, List.of(agg), null, List.of(), 0, 0,
			new org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder(), null, false);

		JsonResultResponse<LiteratureSummaryDocument> ret = new JsonResultResponse<>();
		List<LiteratureSummaryDocument> results = new ArrayList<>();

		ParsedStringTerms terms = searchResponse.getAggregations().get("by_mod");
		for (Terms.Bucket bucket : terms.getBuckets()) {
			TopHits topHits = bucket.getAggregations().get("latest");
			for (SearchHit hit : topHits.getHits().getHits()) {
				LiteratureSummaryDocument doc = mapHit(hit, LiteratureSummaryDocument.class);
				if (doc != null) {
					results.add(doc);
				}
			}
		}

		ret.setTotal(results.size());
		ret.setResults(results);
		return ret;
	}

	private DiseaseAnnotationDocument deserializeDiseaseByCategory(SearchHit hit) {
		Object category = hit.getSourceAsMap().get("category");
		Class<? extends DiseaseAnnotationDocument> klass;
		if ("allele_disease_annotation".equals(category)) {
			klass = AlleleDiseaseAnnotationDocument.class;
		} else if ("agm_disease_annotation".equals(category)) {
			klass = AGMDiseaseAnnotationDocument.class;
		} else {
			klass = GeneDiseaseAnnotationDocument.class;
		}
		DiseaseAnnotationDocument doc = mapHit(hit, klass);
		if (doc == null) {
			return null;
		}
		doc.setUniqueId(hit.getId());
		if (CollectionUtils.isNotEmpty(doc.getPrimaryAnnotations())) {
			doc.setProviders(APIServiceHelper.buildProvidersWithUrl(doc.getPrimaryAnnotations()));
		}
		return doc;
	}

	// agm_phenotype_annotation has no dedicated subclass on this branch; fall back to the
	// base PhenotypeAnnotationDocument for that category.
	private PhenotypeAnnotationDocument deserializePhenotypeByCategory(SearchHit hit) {
		Object category = hit.getSourceAsMap().get("category");
		Class<? extends PhenotypeAnnotationDocument> klass;
		if ("allele_phenotype_annotation".equals(category)) {
			klass = AllelePhenotypeAnnotationDocument.class;
		} else if ("gene_phenotype_annotation".equals(category)) {
			klass = GenePhenotypeAnnotationDocument.class;
		} else {
			klass = PhenotypeAnnotationDocument.class;
		}
		PhenotypeAnnotationDocument doc = mapHit(hit, klass);
		if (doc == null) {
			return null;
		}
		doc.setUniqueId(hit.getId());
		return doc;
	}
}
