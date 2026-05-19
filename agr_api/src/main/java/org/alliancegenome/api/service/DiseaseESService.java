package org.alliancegenome.api.service;

import static java.util.stream.Collectors.toList;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.core.document.AGMDiseaseAnnotationDocument;
import org.alliancegenome.core.document.AlleleDiseaseAnnotationDocument;
import org.alliancegenome.core.document.DiseaseAnnotationDocument;
import org.alliancegenome.api.entity.DiseaseEntitySubgroupSlim;
import org.alliancegenome.api.entity.DiseaseRibbonEntity;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.core.document.GeneDiseaseAnnotationDocument;
import org.alliancegenome.curation_api.model.entities.ontology.DOTerm;
import org.alliancegenome.api.es.dao.SearchDAO;
import org.alliancegenome.api.service.helper.APIServiceHelper;
import org.alliancegenome.api.response.JsonResultResponse;
import org.alliancegenome.curation_api.model.document.es.DiseaseSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.GeneSummaryDocument;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.api.es.dao.DiseaseESDAO;
import org.alliancegenome.api.es.dao.GeneESDAO;
import org.alliancegenome.api.es.query.Pagination;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.IncludeExclude;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequestScoped
public class DiseaseESService extends ESService {

	private DiseaseRibbonService diseaseRibbonService;

	@Inject
	GeneESDAO geneESDAO;

	@Inject
	DiseaseESDAO diseaseESDAO;

	@PostConstruct
	void init() {
		diseaseRibbonService = new DiseaseRibbonService(diseaseESDAO);
	}

	// termID may be used in the future when converting disease page to new ES stack.
	public JsonResultResponse<GeneDiseaseAnnotationDocument> getRibbonDiseaseAnnotations(String focusTaxonId, List<String> geneIDs, String termID, Pagination pagination, boolean excludeNegated, boolean debug) {
		return getRibbonDiseaseAnnotations(focusTaxonId, geneIDs, termID, pagination, excludeNegated, debug, false);
	}

	public JsonResultResponse<GeneDiseaseAnnotationDocument> getRibbonDiseaseAnnotations(String focusTaxonId, List<String> geneIDs, String termID, Pagination pagination, boolean excludeNegated, boolean debug, boolean includePrimaryAnnotations) {
		// unfiltered query
		BoolQueryBuilder query = getBaseQuery(geneIDs, termID, excludeNegated, "gene_disease_annotation", true);

		JsonResultResponse<GeneDiseaseAnnotationDocument> ret = new JsonResultResponse<>();
		ret.setSupplementalData(getSupplementalData(focusTaxonId, true, debug, query));

		// add table filter
		addTableFilter(pagination, query);
		SearchResponse searchResponse = getSearchResponse(query, pagination, getAnnotationSorts(focusTaxonId, debug), false);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<GeneDiseaseAnnotationDocument> list = Arrays.stream(searchResponse.getHits().getHits())
			.map(searchHit -> {
				try {
					GeneDiseaseAnnotationDocument gdad = mapper.readValue(searchHit.getSourceAsString(), GeneDiseaseAnnotationDocument.class);
					gdad.setUniqueId(searchHit.getId());
					gdad.setProviders(APIServiceHelper.buildProvidersWithUrl(gdad.getPrimaryAnnotations()));

					if (!includePrimaryAnnotations) {
						gdad.setPrimaryAnnotations(null);
					}

					return gdad;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}).toList();
		ret.setResults(list);
		return ret;
	}

	public DiseaseSummaryDocument getById(String diseaseId) {

		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("doTerm.curie", diseaseId));
		bool.filter(new TermQueryBuilder("category", "disease_summary"));
		Pagination pagination = new Pagination();
		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, false);
		try {
			if (searchResponse.getHits().getTotalHits().value >= 1) {
				return mapper.readValue(searchResponse.getHits().getHits()[0].getSourceAsString(), DiseaseSummaryDocument.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private Map<String, Object> getSupplementalData(String focusTaxonId, boolean useSpeciesAggregation, boolean debug, BoolQueryBuilder unfilteredQuery) {
		// create histogram of select columns of unfiltered query
		Map<String, String> aggregationFields = new HashMap<>();
		if (useSpeciesAggregation) {
			aggregationFields.put("subject.taxon.species.fullName.keyword", "species");
		}
		aggregationFields.put("generatedRelationString.keyword", "associationType");
		aggregationFields.put("diseaseQualifiers.keyword", "diseaseQualifiers");
		return getSupplementalData(focusTaxonId, useSpeciesAggregation, debug, unfilteredQuery, aggregationFields);
	}

	public JsonResultResponse<AlleleDiseaseAnnotationDocument> getDiseaseAnnotations(String alleleID, Pagination pagination, boolean excludeNegated, boolean debug) {
		// unfiltered base query
		BoolQueryBuilder query = getBaseQuery(List.of(alleleID), null, excludeNegated, "allele_disease_annotation", true);

		JsonResultResponse<AlleleDiseaseAnnotationDocument> ret = new JsonResultResponse<>();
		ret.setSupplementalData(getSupplementalData(null, false, debug, query));

		// add table filter
		addTableFilter(pagination, query);
		SearchResponse searchResponse = getSearchResponse(query, pagination, getAnnotationSorts(null, debug), false);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<AlleleDiseaseAnnotationDocument> list = Arrays.stream(searchResponse.getHits().getHits())
			.map(searchHit -> {
				try {
					AlleleDiseaseAnnotationDocument object = mapper.readValue(searchHit.getSourceAsString(), AlleleDiseaseAnnotationDocument.class);
					object.setUniqueId(searchHit.getId());
					return object;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}).toList();
		ret.setResults(list);
		return ret;

	}

	public DiseaseRibbonSummary getDiseaseRibbonSummary(List<String> geneIDs, Boolean includeNegation, boolean debug) {
		DiseaseRibbonSummary summary = diseaseRibbonService.getDiseaseRibbonSectionInfo();
		Pagination pagination = new Pagination();
		pagination.setLimit(10000);
		// loop over all genes provided
		geneIDs.forEach(geneID -> {
			JsonResultResponse<GeneDiseaseAnnotationDocument> paginationResult = getDiseaseAnnotationList(geneID, pagination, !includeNegation, debug);
			// calculate histogram
			Map<String, List<GeneDiseaseAnnotationDocument>> histogram = getDiseaseAnnotationHistogram(paginationResult);

			GeneSummaryDocument geneDoc = geneESDAO.getById(geneID);
			if (geneDoc == null || geneDoc.getGene() == null) {
				return;
			}
			// populate diseaseEntity records
			populateDiseaseRibbonSummary(geneID, summary, histogram, geneDoc.getGene());
			summary.addAllAnnotationsCount(geneID, paginationResult.getTotal());
		});
		return summary;
	}

	public void populateDiseaseRibbonSummary(String geneID, DiseaseRibbonSummary summary, Map<String, List<GeneDiseaseAnnotationDocument>> histogram, Gene gene) {
		DiseaseRibbonEntity entity = new DiseaseRibbonEntity();
		entity.setId(geneID);
		entity.setLabel(gene.getGeneSymbol().getDisplayText());
		entity.setTaxonID(gene.getTaxon().getCurie());
		entity.setTaxonName(gene.getTaxon().getName());
		summary.addDiseaseRibbonEntity(entity);

		Set<String> allTerms = new HashSet<>();
		Set<GeneDiseaseAnnotationDocument> allAnnotations = new HashSet<>();
		List<String> agrDoSlimIDs = diseaseESDAO.getAgrSlimDocs().stream()
			.filter(d -> d.getDoTerm() != null)
			.map(d -> d.getDoTerm().getCurie())
			.collect(toList());
		// add category term IDs to get the full histogram mapped into the response
		agrDoSlimIDs.addAll(DiseaseRibbonService.slimParentTermIdMap.keySet());
		agrDoSlimIDs.forEach(slimId -> {
			DiseaseEntitySubgroupSlim group = new DiseaseEntitySubgroupSlim();
			int size = 0;
			List<GeneDiseaseAnnotationDocument> diseaseAnnotations = histogram.get(slimId);
			if (diseaseAnnotations != null) {
				allAnnotations.addAll(diseaseAnnotations);
				size = diseaseAnnotations.size();
				Set<String> terms = diseaseAnnotations.stream().map(diseaseAnnotation -> diseaseAnnotation.getObject().getCurie())
					.collect(Collectors.toSet());
				allTerms.addAll(terms);
				group.setNumberOfClasses(terms.size());
			}
			group.setNumberOfAnnotations(size);
			group.setId(slimId);
			if (size > 0) {
				entity.addDiseaseSlim(group);
			}
		});
		entity.setNumberOfClasses(allTerms.size());
		entity.setNumberOfAnnotations(allAnnotations.size());
	}

	private Map<String, List<GeneDiseaseAnnotationDocument>> getDiseaseAnnotationHistogram(JsonResultResponse<GeneDiseaseAnnotationDocument> response) {
		Map<String, List<GeneDiseaseAnnotationDocument>> histogram = new HashMap<>();
		if (CollectionUtils.isEmpty(response.getResults())) {
			return histogram;
		}
		response.getResults().forEach(annotation -> {
			Set<String> parentIDs = annotation.getParentSlimIDs();
			if (parentIDs == null) {
				return;
			}
			parentIDs.forEach(parentID -> {
				histogram.computeIfAbsent(parentID, k -> new ArrayList<>()).add(annotation);
			});
		});
		return histogram;
	}


	private JsonResultResponse<GeneDiseaseAnnotationDocument> getDiseaseAnnotationList(String geneID, Pagination pagination, boolean excludeNegatedAnnotation, boolean debug) {
		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);

		bool.filter(new TermQueryBuilder("category", "gene_disease_annotation"));
		setEntityIdMatcher(geneID, bool2);
		if (excludeNegatedAnnotation) {
			bool.must(matchQuery("primaryAnnotations.negated", false));
		}
		bool.must(matchQuery("viaOrthologyOrder", 0));

		// create histogram of select columns of unfiltered query

		addTableFilter(pagination, bool);

		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, debug);

		JsonResultResponse<GeneDiseaseAnnotationDocument> ret = new JsonResultResponse<>();
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<GeneDiseaseAnnotationDocument> list = new ArrayList<>();

		for (SearchHit searchHit : searchResponse.getHits().getHits()) {
			try {
				GeneDiseaseAnnotationDocument object = mapper.readValue(searchHit.getSourceAsString(), GeneDiseaseAnnotationDocument.class);
				object.setUniqueId(searchHit.getId());
				list.add(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		ret.setResults(list);
		return ret;
	}

	public JsonResultResponse<GeneDiseaseAnnotationDocument> getDiseaseAnnotationsWithGenes(String diseaseID, Pagination pagination, boolean excludeNegatedAnnotation, boolean debug) {
		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);

		bool.filter(new TermQueryBuilder("category", "gene_disease_annotation"));
		bool2.should(new MatchQueryBuilder("parentSlimIDs.keyword", diseaseID));

		JsonResultResponse<GeneDiseaseAnnotationDocument> ret = new JsonResultResponse<>();
		ret.setSupplementalData(getSupplementalData(null, true, debug, bool));

		// create histogram of select columns of unfiltered query
		addTableFilter(pagination, bool);

		// Sorting sets for different names of the sorting selection box
		Map<String, List<String>> sortingSetMap = new HashMap<>();
		sortingSetMap.put("default", List.of("viaOrthologyOrder", "phylogeneticSortingIndex", "subject.geneSymbol.displayText.sort"));
		sortingSetMap.put("gene", List.of("subject.geneSymbol.displayText.sort", "phylogeneticSortingIndex"));
		sortingSetMap.put("disease", List.of("object.name.sort", "phylogeneticSortingIndex", "subject.geneSymbol.displayText.sort"));
		sortingSetMap.put("species", List.of("subject.taxon.species.fullName.keyword", "subject.geneSymbol.displayText.sort"));

		LinkedHashMap<String, SortOrder> sortingMap = new LinkedHashMap<>();

		List<String> sortFields = sortingSetMap.get(pagination.getSortBy());
		if (sortFields == null) {
			sortFields = sortingSetMap.get("default");
		}
		sortFields.forEach(sortField -> sortingMap.put(sortField, SortOrder.ASC));

		SearchResponse searchResponse = getSearchResponse(bool, pagination, sortingMap, debug);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<GeneDiseaseAnnotationDocument> list = new ArrayList<>();

		for (SearchHit searchHit : searchResponse.getHits().getHits()) {
			try {
				GeneDiseaseAnnotationDocument object = mapper.readValue(searchHit.getSourceAsString(), GeneDiseaseAnnotationDocument.class);
				object.setUniqueId(searchHit.getId());
				list.add(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		ret.setResults(list);
		return ret;
	}

	public JsonResultResponse<AGMDiseaseAnnotationDocument> getDiseaseAnnotationsWithModels(String diseaseID, Pagination pagination, boolean excludeNegatedAnnotation, boolean debug) {
		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);

		bool.filter(new TermQueryBuilder("category", "agm_disease_annotation"));
		bool2.should(new MatchQueryBuilder("parentSlimIDs.keyword", diseaseID));

		JsonResultResponse<AGMDiseaseAnnotationDocument> ret = new JsonResultResponse<>();
		ret.setSupplementalData(getSupplementalData(null, true, debug, bool));

		// create histogram of select columns of unfiltered query
		addTableFilter(pagination, bool);

		// Sorting sets for different names of the sorting selection box
		Map<String, List<String>> sortingSetMap = new HashMap<>();
		sortingSetMap.put("default", List.of("phylogeneticSortingIndex", "subject.name.sort"));
		sortingSetMap.put("model", List.of("subject.name.sort", "phylogeneticSortingIndex"));
		sortingSetMap.put("disease", List.of("object.name.sort", "phylogeneticSortingIndex", "subject.name.sort"));
		sortingSetMap.put("species", List.of("subject.taxon.species.fullName.keyword", "subject.name.sort"));

		LinkedHashMap<String, SortOrder> sortingMap = new LinkedHashMap<>();

		List<String> sortFields = sortingSetMap.get(pagination.getSortBy());
		if (sortFields == null) {
			sortFields = sortingSetMap.get("default");
		}
		sortFields.forEach(sortField -> sortingMap.put(sortField, SortOrder.ASC));

		SearchResponse searchResponse = getSearchResponse(bool, pagination, sortingMap, debug);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<AGMDiseaseAnnotationDocument> list = new ArrayList<>();

		for (SearchHit searchHit : searchResponse.getHits().getHits()) {
			try {
				AGMDiseaseAnnotationDocument object = mapper.readValue(searchHit.getSourceAsString(), AGMDiseaseAnnotationDocument.class);
				object.setUniqueId(searchHit.getId());
				list.add(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		ret.setResults(list);
		return ret;
	}

	public JsonResultResponse<AlleleDiseaseAnnotationDocument> getDiseaseAnnotationsWithAlleles(String diseaseID, Pagination pagination) {
		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);

		bool.filter(new TermQueryBuilder("category", "allele_disease_annotation"));
		bool2.should(new MatchQueryBuilder("parentSlimIDs.keyword", diseaseID));

		JsonResultResponse<AlleleDiseaseAnnotationDocument> ret = new JsonResultResponse<>();
		Map<String, Object> supData = getSupplementalData(null, true, false, bool);
		ret.setSupplementalData(supData);

		// create histogram of select columns of unfiltered query
		addTableFilter(pagination, bool);

		// Sorting sets for different names of the sorting selection box
		Map<String, List<String>> sortingSetMap = new HashMap<>();
		sortingSetMap.put("default", List.of("phylogeneticSortingIndex", "subject.alleleSymbol.displayText.sort"));
		sortingSetMap.put("allele", List.of("subject.alleleSymbol.displayText.sort", "phylogeneticSortingIndex"));
		sortingSetMap.put("disease", List.of("object.name.sort", "phylogeneticSortingIndex", "subject.alleleSymbol.displayText.sort"));
		sortingSetMap.put("species", List.of("subject.taxon.species.fullName.keyword", "subject.alleleSymbol.displayText.sort"));

		LinkedHashMap<String, SortOrder> sortingMap = new LinkedHashMap<>();

		List<String> sortFields = sortingSetMap.get(pagination.getSortBy());
		if (sortFields == null) {
			sortFields = sortingSetMap.get("default");
		}
		sortFields.forEach(sortField -> sortingMap.put(sortField, SortOrder.ASC));

		SearchResponse searchResponse = getSearchResponse(bool, pagination, sortingMap, false);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		List<AlleleDiseaseAnnotationDocument> list = new ArrayList<>();

		for (SearchHit searchHit : searchResponse.getHits().getHits()) {
			try {
				AlleleDiseaseAnnotationDocument annotationDocument = mapper.readValue(searchHit.getSourceAsString(), AlleleDiseaseAnnotationDocument.class);
				annotationDocument.setUniqueId(searchHit.getId());
				list.add(annotationDocument);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		ret.setResults(list);
		return ret;
	}

	public JsonResultResponse<DiseaseAnnotation> getDiseasePrimaryAnnotations(String id, Pagination pagination, String category) {
		JsonResultResponse<DiseaseAnnotation> ret = new JsonResultResponse<>();

		BoolQueryBuilder bool = boolQuery();
		bool.must(new TermQueryBuilder("countId", id));
		bool.filter(new TermQueryBuilder("category", category));

		// Not paginating the query here, just getting the document
		Pagination tempPagination = new Pagination();
		tempPagination.setLimit(1);
		SearchResponse response = getSearchResponse(bool, tempPagination, null, false);

		if (response.getHits().getTotalHits().value == 0) {
			ret.setTotal(0);
			ret.setResults(new ArrayList<>());
			return ret;
		}

		try {
			SearchHit hit = response.getHits().getHits()[0];
			DiseaseAnnotationDocument document = mapper.readValue(hit.getSourceAsString(), DiseaseAnnotationDocument.class);
			document.setUniqueId(hit.getId());

			List<DiseaseAnnotation> primaryAnnotations = document.getPrimaryAnnotations();
			if (primaryAnnotations == null) {
				primaryAnnotations = new ArrayList<>();
			}

			// Sort the annotations for consistent ordering and pagination
			List<DiseaseAnnotation> sortedAnnotations = APIServiceHelper.naturalSortByAnnotationSubject(primaryAnnotations);

			// Apply pagination to the sorted results
			int start = pagination.getStart();
			int limit = pagination.getLimit();
			int total = sortedAnnotations.size();

			List<DiseaseAnnotation> paginatedResults = new ArrayList<>();
			if (start < total) {
				int end = Math.min(start + limit, total);
				paginatedResults = sortedAnnotations.subList(start, end);
			}

			ret.setTotal(total);
			ret.setResults(paginatedResults);

		} catch (Exception e) {
			// Log error and return empty result
			ret.setTotal(0);
			ret.setResults(new ArrayList<>());
		}

		return ret;
	}

	public String getAT(String category, String diseaseID) {
		String result = "";

		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);

		bool.filter(new TermQueryBuilder("category", category));
		bool2.should(new MatchQueryBuilder("parentSlimIDs.keyword", diseaseID));

		Map<String, Object> supData = getSupplementalData(null, false, false, bool);
		List<String> assoList = (List<String>) ((Map<String, Map<String, Object>>) supData.get("distinctFieldValues")).get("associationType");
		result = formatAT(assoList);
		return result;
	}

	private static final String DISEASE_ROOT = "DOID:4";
	private static final int MAX_ANCESTOR_DEPTH = 30;

	public List<DOTerm> getAncestors(String diseaseID) {
		List<DOTerm> chain = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		String current = diseaseID;
		for (int i = 0; i < MAX_ANCESTOR_DEPTH; i++) {
			if (current == null || !visited.add(current)) break;
			DiseaseSummaryDocument doc = getById(current);
			if (doc == null || doc.getDoTerm() == null) break;
			chain.add(doc.getDoTerm());
			if (DISEASE_ROOT.equals(current)) break;
			if (doc.getParents() == null || doc.getParents().isEmpty()) break;
			current = doc.getParents().iterator().next().getCurie();
		}
		java.util.Collections.reverse(chain);
		return chain;
	}

	private static final SearchDAO COUNTS_SEARCH_DAO = new SearchDAO();
	private static final SearchDAO BATCH_TERMS_SEARCH_DAO = new SearchDAO();

	public java.util.Map<String, Object> getBatchTerms(java.util.List<String> diseaseIds) {
		java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
		if (diseaseIds == null || diseaseIds.isEmpty()) return result;

		BoolQueryBuilder bool = boolQuery()
			.filter(new TermQueryBuilder("category", "disease_summary"))
			.filter(new TermsQueryBuilder("doTerm.curie.keyword", diseaseIds));

		SearchResponse response = BATCH_TERMS_SEARCH_DAO.performQuery(
			(QueryBuilder) bool, java.util.List.of(), null, java.util.List.of(),
			diseaseIds.size(), 0, new HighlightBuilder(), null, false);

		for (SearchHit hit : response.getHits().getHits()) {
			try {
				DiseaseSummaryDocument doc = mapper.readValue(hit.getSourceAsString(), DiseaseSummaryDocument.class);
				String curie = doc.getDoTerm() != null ? doc.getDoTerm().getCurie() : null;
				if (curie != null) result.put(curie, doc);
			} catch (Exception e) {
				log.error("Failed to deserialize disease term in batch (id={})", hit.getId(), e);
			}
		}
		return result;
	}


	private static final java.util.List<String[]> COUNT_CATEGORIES = java.util.List.of(
		new String[] { "genes", "gene_disease_annotation" },
		new String[] { "models", "agm_disease_annotation" },
		new String[] { "alleles", "allele_disease_annotation" }
	);

	public java.util.Map<String, java.util.Map<String, Long>> getBatchCounts(java.util.List<String> diseaseIds) {
		java.util.Map<String, java.util.Map<String, Long>> result = new java.util.LinkedHashMap<>();
		for (String id : diseaseIds) {
			java.util.Map<String, Long> zeros = new java.util.LinkedHashMap<>();
			for (String[] pair : COUNT_CATEGORIES) zeros.put(pair[0], 0L);
			result.put(id, zeros);
		}
		if (diseaseIds.isEmpty()) return result;
		String[] idArray = diseaseIds.toArray(new String[0]);
		for (String[] pair : COUNT_CATEGORIES) {
			String key = pair[0];
			String category = pair[1];
			java.util.Map<String, Long> counts = countByDisease(category, idArray);
			for (String id : diseaseIds) {
				Long c = counts.get(id);
				if (c != null) result.get(id).put(key, c);
			}
		}
		return result;
	}

	private java.util.Map<String, Long> countByDisease(String category, String[] diseaseIds) {
		BoolQueryBuilder bool = boolQuery()
			.filter(new TermQueryBuilder("category", category))
			.filter(new TermsQueryBuilder("parentSlimIDs.keyword", diseaseIds));

		AggregationBuilder agg = AggregationBuilders
			.terms("by_disease")
			.field("parentSlimIDs.keyword")
			.includeExclude(new IncludeExclude(diseaseIds, null))
			.size(Math.max(diseaseIds.length, 1));

		SearchResponse response = COUNTS_SEARCH_DAO.performQuery(
			(QueryBuilder) bool, java.util.List.of(agg), null, java.util.List.of("subject"),
			0, 0, new HighlightBuilder(), null, false);

		java.util.Map<String, Long> counts = new java.util.HashMap<>();
		ParsedStringTerms terms = response.getAggregations().get("by_disease");
		for (Terms.Bucket bucket : terms.getBuckets()) {
			counts.put(bucket.getKeyAsString(), bucket.getDocCount());
		}
		return counts;
	}


	private String formatAT(List<String> list) {
		if (list.size() == 0) {
			return "";
		}
		if (list.size() == 1) {
			return list.get(0);
		}

		List<String> store = new ArrayList<>();
		store = list.stream().filter(each -> each.indexOf("not") == -1).toList();
		return String.join("|", store);
	}
}
