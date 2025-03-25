package org.alliancegenome.api.service;

import jakarta.enterprise.context.RequestScoped;
import org.alliancegenome.api.entity.*;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.api.service.DiseaseRibbonService;
import org.alliancegenome.curation_api.model.document.es.DiseaseSummaryDocument;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.SimpleTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.alliancegenome.cache.repository.helper.JsonResultResponse.DISTINCT_FIELD_VALUES;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;


@RequestScoped
public class DiseaseESService extends ESService {

	private static final GeneRepository geneRepository = new GeneRepository();
	private static final DiseaseRepository diseaseRepository = new DiseaseRepository();
	private static final DiseaseRibbonService diseaseRibbonService = new DiseaseRibbonService(diseaseRepository);

	// termID may be used in the future when converting disease page to new ES stack.
	public JsonResultResponse<GeneDiseaseAnnotationDocument> getRibbonDiseaseAnnotations(String focusTaxonId, List<String> geneIDs, String termID, Pagination pagination, boolean excludeNegated, boolean debug) {

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
					GeneDiseaseAnnotationDocument object = mapper.readValue(searchHit.getSourceAsString(), GeneDiseaseAnnotationDocument.class);
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

	public DiseaseSummaryDocument getById(String diseaseId) {

		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("doTerm.curie", diseaseId));
		bool.filter(new TermQueryBuilder("category", "disease_summary"));
		DiseaseSummaryDocument diseaseSummary = new DiseaseSummaryDocument();
		Pagination pagination = new Pagination();
		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, false);
		try {
			diseaseSummary = mapper.readValue(searchResponse.getHits().getHits()[0].getSourceAsString(), DiseaseSummaryDocument.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return diseaseSummary;
	}

	private Map<String, Object> getSupplementalData(String focusTaxonId, boolean useSpeciesAggregation, boolean debug, BoolQueryBuilder unfilteredQuery) {
		// create histogram of select columns of unfiltered query
		Map<String, String> aggregationFields = new HashMap<>();
		if (useSpeciesAggregation) {
			aggregationFields.put("subject.taxon.name.keyword", "species");
		}
		aggregationFields.put("generatedRelationString.keyword", "associationType");
		aggregationFields.put("diseaseQualifiers.keyword", "diseaseQualifiers");
		Map<String, List<String>> distinctFieldValueMap = getAggregations(unfilteredQuery, aggregationFields, focusTaxonId, useSpeciesAggregation, debug);
		Map<String, Object> supplementalData = new LinkedHashMap<>();
		supplementalData.put(DISTINCT_FIELD_VALUES, distinctFieldValueMap);
		return supplementalData;
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

			Gene gene = geneRepository.getShallowGene(geneID);
			if (gene == null) {
				return;
			}
			// populate diseaseEntity records
			populateDiseaseRibbonSummary(geneID, summary, histogram, gene);
			summary.addAllAnnotationsCount(geneID, paginationResult.getTotal());
		});
		return summary;
	}

	public void populateDiseaseRibbonSummary(String geneID, DiseaseRibbonSummary summary, Map<String, List<GeneDiseaseAnnotationDocument>> histogram, Gene gene) {
		DiseaseRibbonEntity entity = new DiseaseRibbonEntity();
		entity.setId(geneID);
		entity.setLabel(gene.getSymbol());
		entity.setTaxonID(gene.getTaxonId());
		entity.setTaxonName(gene.getSpecies().getName());
		summary.addDiseaseRibbonEntity(entity);

		Set<String> allTerms = new HashSet<>();
		Set<GeneDiseaseAnnotationDocument> allAnnotations = new HashSet<>();
		List<String> agrDoSlimIDs = diseaseRepository.getAgrDoSlim().stream()
			.map(SimpleTerm::getPrimaryKey)
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
			Set<String> parentIDs = diseaseRibbonService.getAllParentIDs(annotation.getObject().getCurie());
			parentIDs.forEach(parentID -> {
				List<GeneDiseaseAnnotationDocument> list = histogram.get(parentID);
				if (list == null) {
					list = new ArrayList<>();
				}
				list.add(annotation);
				histogram.put(parentID, list);
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
		sortingSetMap.put("species", List.of("subject.taxon.name.keyword", "subject.geneSymbol.displayText.sort"));

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
		sortingSetMap.put("species", List.of("subject.taxon.name.keyword", "subject.name.sort"));

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
		sortingSetMap.put("species", List.of("subject.taxon.name.keyword", "subject.alleleSymbol.displayText.sort"));

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
}
