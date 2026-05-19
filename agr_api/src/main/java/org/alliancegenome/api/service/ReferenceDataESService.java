package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import org.alliancegenome.api.entity.AGMDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.AGMPhenotypeAnnotationDocument;
import org.alliancegenome.api.entity.AlleleDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.AllelePhenotypeAnnotationDocument;
import org.alliancegenome.api.entity.DiseaseAnnotationDocument;
import org.alliancegenome.api.entity.GeneDiseaseAnnotationDocument;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.api.entity.GeneMolecularInteractionDocument;
import org.alliancegenome.api.entity.GenePhenotypeAnnotationDocument;
import org.alliancegenome.api.entity.PhenotypeAnnotationDocument;
import org.alliancegenome.api.service.helper.APIServiceHelper;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.SearchHit;

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

	public JsonResultResponse<DiseaseAnnotationDocument> getDiseaseAnnotations(String referenceCurie, Pagination pagination) {
		BoolQueryBuilder query = boolQuery()
			.filter(termsQuery("category", DISEASE_CATEGORIES))
			.must(termQuery("references.curie.keyword", referenceCurie));

		return runQuery(query, pagination, this::deserializeDiseaseByCategory);
	}

	public JsonResultResponse<PhenotypeAnnotationDocument> getPhenotypeAnnotations(String referenceCurie, Pagination pagination) {
		BoolQueryBuilder query = boolQuery()
			.filter(termsQuery("category", PHENOTYPE_CATEGORIES))
			.must(termQuery("references.curie.keyword", referenceCurie));

		return runQuery(query, pagination, this::deserializePhenotypeByCategory);
	}

	// Expression docs on stage index only `referenceId` (PMID/MOD IDs).
	// The caller passes the set of cross-reference curies (PMID/MOD) from the literature summary.
	public JsonResultResponse<GeneExpressionDocument> getExpressionAnnotations(List<String> crossReferenceCuries, Pagination pagination) {
		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", "gene_expression_annotation"));
		if (crossReferenceCuries != null && !crossReferenceCuries.isEmpty()) {
			query.must(termsQuery("referenceId.keyword", crossReferenceCuries));
		}
		return runQuery(query, pagination, hit -> mapHit(hit, GeneExpressionDocument.class));
	}

	// Molecular interaction docs only index `geneMolecularInteraction.evidence.referenceID` (PMID/MOD IDs).
	public JsonResultResponse<GeneMolecularInteractionDocument> getMolecularInteractions(List<String> crossReferenceCuries, Pagination pagination) {
		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", "gene_molecular_interaction"));
		if (crossReferenceCuries != null && !crossReferenceCuries.isEmpty()) {
			query.must(termsQuery("geneMolecularInteraction.evidence.referenceID.keyword", crossReferenceCuries));
		}
		return runQuery(query, pagination, hit -> mapHit(hit, GeneMolecularInteractionDocument.class));
	}

	// Genetic interaction category mirrors molecular interaction shape. Note: stage ES currently has
	// zero docs in `gene_genetic_interaction` (indexing gap); code works once the category is populated.
	public JsonResultResponse<org.alliancegenome.api.entity.GeneGeneticInteractionDocument> getGeneticInteractions(List<String> crossReferenceCuries, Pagination pagination) {
		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", "gene_genetic_interaction"));
		if (crossReferenceCuries != null && !crossReferenceCuries.isEmpty()) {
			query.must(termsQuery("geneGeneticInteraction.evidence.referenceID.keyword", crossReferenceCuries));
		}
		return runQuery(query, pagination, hit -> mapHit(hit, org.alliancegenome.api.entity.GeneGeneticInteractionDocument.class));
	}

	public JsonResultResponse<Map<String, Object>> getGenesByReference(String referenceCurie) {
		return getDistinctSubjects(GENE_SUBJECT_CATEGORIES, referenceCurie);
	}

	private static final List<String> GENE_AND_ALLELE_ANNOTATION_CATEGORIES = List.of(
		"gene_disease_annotation",
		"gene_phenotype_annotation",
		"allele_disease_annotation",
		"allele_phenotype_annotation"
	);

	// Related papers by Jaccard similarity of gene subjects.
	// - A = distinct gene subjects on this reference (optionally expanded with orthologs)
	// - For candidates sharing any of A, B = their own distinct gene subjects
	// - jaccard = |A ∩ B| / |A ∪ B| = shared / (|A| + |B| - shared)
	public JsonResultResponse<Map<String, Object>> getRelatedPapers(String referenceCurie, int limit, boolean includeOrthologs) {
		JsonResultResponse<Map<String, Object>> genesResp = getDistinctSubjects(GENE_SUBJECT_CATEGORIES, referenceCurie);
		List<String> focusGenes = new ArrayList<>();
		for (Map<String, Object> g : genesResp.getResults()) {
			Object id = g.get("primaryExternalId");
			if (id != null) focusGenes.add(id.toString());
		}

		List<String> aGenes = new ArrayList<>(focusGenes);
		if (includeOrthologs && !focusGenes.isEmpty()) {
			aGenes.addAll(getOrthologGeneIds(focusGenes));
			aGenes = new ArrayList<>(aGenes.stream().distinct().toList());
		}
		int aSize = aGenes.size();

		JsonResultResponse<Map<String, Object>> ret = new JsonResultResponse<>();
		if (aGenes.isEmpty()) {
			ret.setTotal(0);
			ret.setResults(List.of());
			return ret;
		}

		// 1) find candidate papers and their shared-gene count
		BoolQueryBuilder sharedQuery = boolQuery()
			.filter(termsQuery("category", GENE_AND_ALLELE_ANNOTATION_CATEGORIES))
			.must(termsQuery("subject.primaryExternalId.keyword", aGenes));

		AggregationBuilder sharedAgg = AggregationBuilders
			.terms("refs")
			.field("references.curie.keyword")
			.size(Math.max(50, limit * 3))
			.subAggregation(AggregationBuilders.cardinality("uniqGenes").field("subject.primaryExternalId.keyword"))
			.subAggregation(AggregationBuilders.terms("species").field("subject.taxon.species.fullName.keyword").size(10));

		SearchResponse sharedResp = SEARCH_DAO.performQuery(
			(QueryBuilder) sharedQuery, List.of(sharedAgg), null, List.of(),
			0, 0, new org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder(), null, false);

		Map<String, Integer> sharedCounts = new java.util.LinkedHashMap<>();
		Map<String, List<String>> sharedSpecies = new java.util.HashMap<>();
		ParsedStringTerms refTerms = sharedResp.getAggregations().get("refs");
		for (Terms.Bucket b : refTerms.getBuckets()) {
			String candidateRef = b.getKeyAsString();
			if (candidateRef.equals(referenceCurie)) continue;
			// Skip external disease-database refs (OMIM, Orphanet) that aren't papers.
			if (!candidateRef.startsWith("AGRKB:")) continue;
			org.elasticsearch.search.aggregations.metrics.Cardinality card = b.getAggregations().get("uniqGenes");
			sharedCounts.put(candidateRef, (int) card.getValue());
			ParsedStringTerms speciesAgg = b.getAggregations().get("species");
			List<String> species = new ArrayList<>();
			for (Terms.Bucket sb : speciesAgg.getBuckets()) {
				species.add(sb.getKeyAsString());
			}
			sharedSpecies.put(candidateRef, species);
		}
		if (sharedCounts.isEmpty()) {
			ret.setTotal(0);
			ret.setResults(List.of());
			return ret;
		}

		// 2) for the candidates, get each one's own distinct gene count (|B|)
		BoolQueryBuilder bSizeQuery = boolQuery()
			.filter(termsQuery("category", GENE_AND_ALLELE_ANNOTATION_CATEGORIES))
			.must(termsQuery("references.curie.keyword", sharedCounts.keySet().stream().toList()));

		AggregationBuilder bSizeAgg = AggregationBuilders
			.terms("refs")
			.field("references.curie.keyword")
			.size(sharedCounts.size() + 1)
			.subAggregation(AggregationBuilders.cardinality("uniqGenes").field("subject.primaryExternalId.keyword"));

		SearchResponse bSizeResp = SEARCH_DAO.performQuery(
			(QueryBuilder) bSizeQuery, List.of(bSizeAgg), null, List.of(),
			0, 0, new org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder(), null, false);

		Map<String, Integer> bSizes = new java.util.HashMap<>();
		ParsedStringTerms bTerms = bSizeResp.getAggregations().get("refs");
		for (Terms.Bucket b : bTerms.getBuckets()) {
			org.elasticsearch.search.aggregations.metrics.Cardinality card = b.getAggregations().get("uniqGenes");
			bSizes.put(b.getKeyAsString(), (int) card.getValue());
		}

		// 3) compute Jaccard and sort
		record Scored(String curie, int shared, int bSize, double jaccard) {}
		List<Scored> scored = new ArrayList<>();
		for (Map.Entry<String, Integer> e : sharedCounts.entrySet()) {
			int shared = e.getValue();
			int b = bSizes.getOrDefault(e.getKey(), shared);
			int union = aSize + b - shared;
			double j = union == 0 ? 0 : ((double) shared) / union;
			scored.add(new Scored(e.getKey(), shared, b, j));
		}
		scored.sort((x, y) -> Double.compare(y.jaccard(), x.jaccard()));

		List<Map<String, Object>> out = new ArrayList<>();
		for (Scored s : scored.stream().limit(limit).toList()) {
			Map<String, Object> row = new java.util.LinkedHashMap<>();
			row.put("referenceCurie", s.curie());
			row.put("sharedGenes", s.shared());
			row.put("candidateGeneCount", s.bSize());
			row.put("focusGeneCount", aSize);
			row.put("jaccard", s.jaccard());
			row.put("sharedSpecies", sharedSpecies.getOrDefault(s.curie(), List.of()));
			out.add(row);
		}
		ret.setTotal(out.size());
		ret.setResults(out);
		return ret;
	}

	// Returns the object-side gene IDs for orthology rows whose subject is in the input set.
	// objectGene.primaryExternalId has no .keyword sub-field in the stage mapping, so we
	// fetch source docs and pull the ID out of _source rather than using a terms aggregation.
	private List<String> getOrthologGeneIds(List<String> subjectGeneIds) {
		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", "gene_to_gene_orthology"))
			.must(termsQuery("geneToGeneOrthologyGenerated.subjectGene.primaryExternalId.keyword", subjectGeneIds));

		SearchResponse resp = SEARCH_DAO.performQuery(
			(QueryBuilder) query, List.of(), null,
			List.of("geneToGeneOrthologyGenerated.objectGene.primaryExternalId"),
			10_000, 0,
			new org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder(), null, false);

		java.util.Set<String> out = new java.util.LinkedHashSet<>();
		for (SearchHit hit : resp.getHits().getHits()) {
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> gto = (Map<String, Object>) hit.getSourceAsMap().get("geneToGeneOrthologyGenerated");
				if (gto == null) continue;
				@SuppressWarnings("unchecked")
				Map<String, Object> obj = (Map<String, Object>) gto.get("objectGene");
				if (obj == null) continue;
				Object id = obj.get("primaryExternalId");
				if (id != null) out.add(id.toString());
			} catch (Exception e) {
				log.warn("Failed to parse ortholog object gene (hit id={})", hit.getId(), e);
			}
		}
		return new ArrayList<>(out);
	}

	// Orthologs of the genes mentioned in this reference. "Reference-scoped" loosely —
	// gene_to_gene_orthology docs have no reference field, so we look up distinct genes
	// first, then fetch their orthologs.
	public JsonResultResponse<Map<String, Object>> getOrthologyByReference(String referenceCurie, Pagination pagination) {
		JsonResultResponse<Map<String, Object>> genesResp = getDistinctSubjects(GENE_SUBJECT_CATEGORIES, referenceCurie);
		List<String> geneIds = new ArrayList<>();
		for (Map<String, Object> gene : genesResp.getResults()) {
			Object id = gene.get("primaryExternalId");
			if (id != null) geneIds.add(id.toString());
		}

		JsonResultResponse<Map<String, Object>> ret = new JsonResultResponse<>();
		if (geneIds.isEmpty()) {
			ret.setTotal(0);
			ret.setResults(List.of());
			return ret;
		}

		BoolQueryBuilder query = boolQuery()
			.filter(termQuery("category", "gene_to_gene_orthology"))
			.must(termsQuery("geneToGeneOrthologyGenerated.subjectGene.primaryExternalId.keyword", geneIds));

		addTableFilter(pagination, query);
		SearchResponse searchResponse = getSearchResponse(query, pagination, null, false);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<Map<String, Object>> results = new ArrayList<>();
		for (SearchHit hit : searchResponse.getHits().getHits()) {
			results.add(hit.getSourceAsMap());
		}
		ret.setResults(results);
		return ret;
	}

	public JsonResultResponse<Map<String, Object>> getAllelesByReference(String referenceCurie) {
		return getDistinctSubjects(ALLELE_SUBJECT_CATEGORIES, referenceCurie);
	}

	public JsonResultResponse<Map<String, Object>> getModelsByReference(String referenceCurie) {
		return getDistinctSubjects(MODEL_SUBJECT_CATEGORIES, referenceCurie);
	}

	private static final SearchDAO SEARCH_DAO = new SearchDAO();

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

	@FunctionalInterface
	private interface HitMapper<T> {
		T map(SearchHit hit);
	}

	private <T> JsonResultResponse<T> runQuery(BoolQueryBuilder query, Pagination pagination, HitMapper<T> mapper) {
		addTableFilter(pagination, query);
		SearchResponse searchResponse = getSearchResponse(query, pagination, null, false);

		JsonResultResponse<T> ret = new JsonResultResponse<>();
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<T> results = new ArrayList<>();
		for (SearchHit hit : searchResponse.getHits().getHits()) {
			T doc = mapper.map(hit);
			if (doc != null) {
				results.add(doc);
			}
		}
		ret.setResults(results);
		return ret;
	}

	private <T> T mapHit(SearchHit hit, Class<T> type) {
		try {
			return this.mapper.readValue(hit.getSourceAsString(), type);
		} catch (Exception e) {
			log.error("Failed to deserialize hit id={} as {}", hit.getId(), type.getSimpleName(), e);
			return null;
		}
	}

	private DiseaseAnnotationDocument deserializeDiseaseByCategory(SearchHit hit) {
		try {
			Object category = hit.getSourceAsMap().get("category");
			String json = hit.getSourceAsString();
			DiseaseAnnotationDocument doc;
			if ("allele_disease_annotation".equals(category)) {
				doc = mapper.readValue(json, AlleleDiseaseAnnotationDocument.class);
			} else if ("agm_disease_annotation".equals(category)) {
				doc = mapper.readValue(json, AGMDiseaseAnnotationDocument.class);
			} else {
				doc = mapper.readValue(json, GeneDiseaseAnnotationDocument.class);
			}
			doc.setUniqueId(hit.getId());
			if (CollectionUtils.isNotEmpty(doc.getPrimaryAnnotations())) {
				doc.setProviders(APIServiceHelper.buildProvidersWithUrl(doc.getPrimaryAnnotations()));
			}
			return doc;
		} catch (Exception e) {
			log.error("Failed to deserialize disease annotation hit id={}", hit.getId(), e);
			return null;
		}
	}

	private PhenotypeAnnotationDocument deserializePhenotypeByCategory(SearchHit hit) {
		try {
			Object category = hit.getSourceAsMap().get("category");
			String json = hit.getSourceAsString();
			PhenotypeAnnotationDocument doc;
			if ("allele_phenotype_annotation".equals(category)) {
				doc = mapper.readValue(json, AllelePhenotypeAnnotationDocument.class);
			} else if ("agm_phenotype_annotation".equals(category)) {
				doc = mapper.readValue(json, AGMPhenotypeAnnotationDocument.class);
			} else {
				doc = mapper.readValue(json, GenePhenotypeAnnotationDocument.class);
			}
			doc.setUniqueId(hit.getId());
			return doc;
		} catch (Exception e) {
			log.error("Failed to deserialize phenotype annotation hit id={}", hit.getId(), e);
			return null;
		}
	}
}
