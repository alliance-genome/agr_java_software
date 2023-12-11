package org.alliancegenome.api.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.alliancegenome.api.entity.*;
import org.alliancegenome.api.service.helper.GeneDiseaseSearchHelper;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.api.service.DiseaseRibbonService;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.SimpleTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.alliancegenome.cache.repository.helper.JsonResultResponse.DISTINCT_FIELD_VALUES;
import static org.elasticsearch.index.query.QueryBuilders.*;


@RequestScoped
public class DiseaseESService {

	@Inject
	ObjectMapper mapper;

	private static final GeneRepository geneRepository = new GeneRepository();
	private static final DiseaseRepository diseaseRepository = new DiseaseRepository();
	private static final DiseaseRibbonService diseaseRibbonService = new DiseaseRibbonService(diseaseRepository);
	private static final SearchDAO searchDAO = new SearchDAO();
	private static final GeneDiseaseSearchHelper geneDiseaseSearchHelper = new GeneDiseaseSearchHelper();

	// termID may be used in the future when converting disease page to new ES stack.
	public JsonResultResponse<GeneDiseaseAnnotationDocument> getRibbonDiseaseAnnotations(String focusTaxonId, List<String> geneIDs, String termID, Pagination pagination, boolean excludeNegated, boolean debug) {

		// unfiltered query
		BoolQueryBuilder query = getBaseQuery(geneIDs, termID, pagination, excludeNegated, "gene_disease_annotation");

		JsonResultResponse<GeneDiseaseAnnotationDocument> ret = new JsonResultResponse<>();
		ret.setSupplementalData(getSupplementalData(focusTaxonId, debug, query));

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

	private Map<String, Object> getSupplementalData(String focusTaxonId, boolean debug, BoolQueryBuilder unfilteredQuery) {
		// create histogram of select columns of unfiltered query
		Map<String, String> aggregationFields = new HashMap<>();
		if (StringUtils.isNotEmpty(focusTaxonId)) {
			aggregationFields.put("subject.taxon.name.keyword", "species");
		}
		aggregationFields.put("generatedRelationString.keyword", "associationType");
		aggregationFields.put("diseaseQualifiers.keyword", "diseaseQualifiers");
		Map<String, List<String>> distinctFieldValueMap = getAggregations(unfilteredQuery, aggregationFields, focusTaxonId, debug);
		Map<String, Object> supplementalData = new LinkedHashMap<>();
		supplementalData.put(DISTINCT_FIELD_VALUES, distinctFieldValueMap);
		return supplementalData;
	}

	private SearchResponse getSearchResponse(BoolQueryBuilder bool, Pagination pagination, LinkedHashMap<String, SortOrder> focusTaxonId, boolean debug) {
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();

		return searchDAO.performQuery(
			bool, aggBuilders, null, geneDiseaseSearchHelper.getResponseFields(),
			pagination.getLimit(), pagination.getOffset(), hlb, focusTaxonId, debug);
	}

	private BoolQueryBuilder getBaseQuery(List<String> entityIDs, String termID, Pagination pagination, boolean excludeNegated, String recordType) {
		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);

		bool.filter(termQuery("category", recordType));

		if (CollectionUtils.isNotEmpty(entityIDs)) {
			for (String geneId : entityIDs) {
				bool2.should(new MatchQueryBuilder("subject.curie.keyword", geneId));
			}
		}
		if (excludeNegated) {
			bool.must(matchQuery("primaryAnnotations.negated", false));
		}
		if (termID != null) {
			BoolQueryBuilder bool3 = boolQuery();
			bool.must(bool3);
			if (termID.equals(DiseaseRibbonSummary.DOID_OTHER)) {
				BoolQueryBuilder orClause = boolQuery();
				DOTerm.getAllOtherDiseaseTerms().forEach(parentID -> orClause.should(QueryBuilders.termQuery("parentSlimIDs.keyword", parentID)));
				bool3.should(orClause);

			} else {
				bool3.should(new MatchQueryBuilder("parentSlimIDs.keyword", termID));
			}
		}
		return bool;
	}

	private void addTableFilter(Pagination pagination, BoolQueryBuilder bool) {
		HashMap<String, String> filterOptionMap = pagination.getFilterOptionMap();
		if (MapUtils.isNotEmpty(filterOptionMap)) {
			filterOptionMap.forEach((filterName, filterValue) -> generateFilter(bool, filterName, filterValue));
		}
	}

	public JsonResultResponse<AlleleDiseaseAnnotationDocument> getDiseaseAnnotations(String alleleID,
																					 Pagination pagination,
																					 boolean excludeNegated,
																					 boolean debug) {
		// unfiltered base query
		BoolQueryBuilder query = getBaseQuery(List.of(alleleID), null, pagination, excludeNegated, "allele_disease_annotation");

		JsonResultResponse<AlleleDiseaseAnnotationDocument> ret = new JsonResultResponse<>();
		ret.setSupplementalData(getSupplementalData(null, debug, query));

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

	private void generateFilter(BoolQueryBuilder bool, String filterName, String filterValue) {
		if (filterValue.contains("|")) {
			//Log.info("Or Filter: " + filterName + " " + filterValue);
			BoolQueryBuilder orClause = boolQuery();
			String[] elements = filterValue.split("\\|");
			Arrays.stream(elements).forEach(element -> orClause.should(QueryBuilders.termQuery(filterName, escapeValue(element))));
			bool.must(orClause);
		} else {
			//Log.info("Other Filter: " + filterName + " " + filterValue);
			if (filterName.endsWith("keyword")) {
				bool.must(QueryBuilders.termQuery(filterName, filterValue));
			} else {
				if (filterName.contains("OR")) {
					BoolQueryBuilder outerAndClause = boolQuery();
					String[] filterNames = filterName.split("OR");
					Arrays.stream(filterNames).forEach(indivFilterName -> {
						BoolQueryBuilder orClause = getBooleanAndedQueryBuilder(indivFilterName.trim(), filterValue);
						outerAndClause.should(orClause);
					});
					bool.must(outerAndClause);
				} else {
					BoolQueryBuilder andClause = getBooleanAndedQueryBuilder(filterName, filterValue);
					bool.must(andClause);
				}
			}
		}
		//Log.info(bool);
	}

	/*
	 * split filter values by white spaces and create and ANDed boolean query
	 */
	private BoolQueryBuilder getBooleanAndedQueryBuilder(String filterName, String filterValue) {
		BoolQueryBuilder andClause = boolQuery();
		String[] elements = escapeValue(filterValue).split(" ");
		Arrays.stream(elements).forEach(element -> andClause.must(QueryBuilders.queryStringQuery("*" + element + "*").field(filterName)));
		return andClause;
	}

	private String escapeValue(String value) {
		value = value.replaceAll("'", " ");
		value = QueryParser.escape(value);
		return value;
	}

	private Map<String, List<String>> getAggregations(BoolQueryBuilder bool, Map<String, String> aggregationFields, String focusTaxonId, boolean debug) {
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		aggregationFields.forEach((field, colName) -> {
			String fieldNameAgg = field + "_agg";
			TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms(fieldNameAgg);
			aggregationBuilder.bucketCardinality();
			aggregationBuilder.field(field);
			aggBuilders.add(aggregationBuilder);
		});

		SearchResponse searchResponseHistogram = searchDAO.performQuery(
			bool, aggBuilders, null, geneDiseaseSearchHelper.getResponseFields(),
			0, 0, new HighlightBuilder(), focusTaxonId == null ? null : getAnnotationSorts(focusTaxonId, debug), debug);

		Map<String, List<String>> distinctFieldValueMap = new HashMap<>();
		aggregationFields.forEach((field, colName) -> {
			String fieldNameAgg = field + "_agg";
			List<String> values = ((ParsedStringTerms) searchResponseHistogram.getAggregations().get(fieldNameAgg)).getBuckets().stream()
				.map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList());
			distinctFieldValueMap.put(colName, values);
		});
		return distinctFieldValueMap;
	}

	private LinkedHashMap<String, SortOrder> getAnnotationSorts(String focusTaxonId, boolean debug) {
		SpeciesType type = SpeciesType.getTypeByID(focusTaxonId);
		LinkedHashMap<String, SortOrder> sorts = new LinkedHashMap<>();
		if (type != null) {
			sorts.put("speciesOrder." + type.getTaxonIDPart(), SortOrder.ASC);
		} else {
			if (debug) {
				Log.info("Species could not be found for: " + focusTaxonId);
			} else {
				Log.debug("Species could not be found for: " + focusTaxonId);
			}
		}
		sorts.put("object.name.sort", SortOrder.ASC);
		if (debug) Log.info(sorts);
		return sorts;
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
			if (gene == null)
				return;
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
			if (size > 0)
				entity.addDiseaseSlim(group);
		});
		entity.setNumberOfClasses(allTerms.size());
		entity.setNumberOfAnnotations(allAnnotations.size());
	}

	private Map<String, List<GeneDiseaseAnnotationDocument>> getDiseaseAnnotationHistogram(JsonResultResponse<GeneDiseaseAnnotationDocument> response) {
		Map<String, List<GeneDiseaseAnnotationDocument>> histogram = new HashMap<>();
		if (CollectionUtils.isEmpty(response.getResults()))
			return histogram;
		response.getResults().forEach(annotation -> {
			Set<String> parentIDs = diseaseRibbonService.getAllParentIDs(annotation.getObject().getCurie());
			parentIDs.forEach(parentID -> {
				List<GeneDiseaseAnnotationDocument> list = histogram.get(parentID);
				if (list == null)
					list = new ArrayList<>();
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
		bool2.should(new MatchQueryBuilder("subject.curie.keyword", geneID));
		if (excludeNegatedAnnotation) {
			bool.must(matchQuery("primaryAnnotations.negated", false));
		}

		// create histogram of select columns of unfiltered query

		addTableFilter(pagination, bool);

		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, debug);

		JsonResultResponse<GeneDiseaseAnnotationDocument> ret = new JsonResultResponse<>();
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<GeneDiseaseAnnotationDocument> list = new ArrayList<>();
		ObjectMapper mapper2 = new ObjectMapper();
		JavaTimeModule module = new JavaTimeModule();
		mapper2.registerModule(module);
		mapper2.registerModule(new Jdk8Module());

		mapper2.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper2.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper2.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

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

		// create histogram of select columns of unfiltered query

		addTableFilter(pagination, bool);

		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, debug);

		JsonResultResponse<GeneDiseaseAnnotationDocument> ret = new JsonResultResponse<>();
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<GeneDiseaseAnnotationDocument> list = new ArrayList<>();
		ObjectMapper mapper2 = new ObjectMapper();
		JavaTimeModule module = new JavaTimeModule();
		mapper2.registerModule(module);
		mapper2.registerModule(new Jdk8Module());

		mapper2.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper2.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper2.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

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
}
