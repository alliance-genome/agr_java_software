package org.alliancegenome.api.service;

import static java.util.stream.Collectors.toList;
import static org.alliancegenome.cache.repository.helper.JsonResultResponse.DISTINCT_FIELD_VALUES;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.entity.DiseaseEntitySubgroupSlim;
import org.alliancegenome.api.entity.DiseaseRibbonEntity;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.entity.GeneDiseaseAnnotationDocument;
import org.alliancegenome.api.service.helper.GeneDiseaseSearchHelper;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.api.service.DiseaseRibbonService;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.SimpleTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkus.logging.Log;


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
	public JsonResultResponse<GeneDiseaseAnnotationDocument> getRibbonDiseaseAnnotations(List<String> geneIDs, String termID, Pagination pagination, boolean excludeNegated) {

		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);

		bool.filter(termQuery("category", "gene_disease_annotation"));

		for (String geneId : geneIDs) {
			bool2.should(new MatchQueryBuilder("subject.curie.keyword", geneId));
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

		// create histogram of select columns of unfiltered query
		Map<String, String> aggregationFields = new HashMap<>();
		aggregationFields.put("subject.taxon.name.keyword", "species");
		aggregationFields.put("diseaseRelationNegation.keyword", "associationType");
		Map<String, List<String>> distinctFieldValueMap = addAggregations(bool, aggregationFields);

		HashMap<String, String> filterOptionMap = pagination.getFilterOptionMap();
		if (MapUtils.isNotEmpty(filterOptionMap)) {
			filterOptionMap.forEach((filterName, filterValue) -> {
				generateFilter(bool, filterName, filterValue);
			});
		}

		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();
		SearchResponse searchResponse = searchDAO.performQuery(
			bool, aggBuilders, null, geneDiseaseSearchHelper.getResponseFields(),
			pagination.getLimit(), pagination.getOffset(), hlb, "diseaseAnnotation", false);

		JsonResultResponse<GeneDiseaseAnnotationDocument> ret = new JsonResultResponse<>();
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		Map<String, Object> supplementalData = new LinkedHashMap<>();
		supplementalData.put(DISTINCT_FIELD_VALUES, distinctFieldValueMap);
		ret.setSupplementalData(supplementalData);

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

	private void generateFilter(BoolQueryBuilder bool, String filterName, String filterValue) {

		if (filterValue.contains("|")) {
			Log.info("Or Filter: " + filterName + " " + filterValue);
			BoolQueryBuilder orClause = boolQuery();
			String[] elements = filterValue.split("\\|");
			Arrays.stream(elements).forEach(element -> orClause.should(QueryBuilders.termQuery(filterName, element)));
			bool.must(orClause);
		} else if(filterValue.contains("&")) {
			Log.info("And Filter: " + filterName + " " + filterValue);

			String[] elements = filterValue.split("\\&");
			//Arrays.stream(elements).forEach(element -> andClause.should(QueryBuilders.termQuery(filterName, element)));
			StringBuffer queryString = new StringBuffer();
			String delim = "";
			for(String element: elements) {
				queryString.append(delim);
				queryString.append(element);
				delim = " ";
			}
			
			bool.must(QueryBuilders.queryStringQuery(queryString.toString()).defaultOperator(Operator.AND).field(filterName));
		} else {
			Log.info("Other Filter: " + filterName + " " + filterValue);
			
			if (filterName.endsWith("keyword")) {
				bool.must(QueryBuilders.termQuery(filterName, filterValue));
			} else {
				String[] elements = filterValue.split(" ");
				BoolQueryBuilder andClause = boolQuery();
				Arrays.stream(elements).forEach(element -> andClause.must(QueryBuilders.queryStringQuery("*" + element + "*").field(filterName)));
				bool.must(andClause);
			}
		}
		Log.info(bool);
	}

	private Map<String, List<String>> addAggregations(BoolQueryBuilder bool, Map<String, String> aggregationFields) {
		Map<String, List<String>> distinctFieldValueMap = new HashMap<>();
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
			0, 0, new HighlightBuilder(), "diseaseAnnotation", false);

		aggregationFields.forEach((field, colName) -> {
			String fieldNameAgg = field + "_agg";
			List<String> values = ((ParsedStringTerms) searchResponseHistogram.getAggregations().get(fieldNameAgg)).getBuckets().stream()
				.map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList());
			distinctFieldValueMap.put(colName, values);
		});
		return distinctFieldValueMap;
	}

	public DiseaseRibbonSummary getDiseaseRibbonSummary(List<String> geneIDs, String includeNegation) {
		DiseaseRibbonSummary summary = diseaseRibbonService.getDiseaseRibbonSectionInfo();
		Pagination pagination = new Pagination();
		pagination.setLimit(10000);
		pagination.addFieldFilter(FieldFilter.INCLUDE_NEGATION, includeNegation);
		// loop over all genes provided
		geneIDs.forEach(geneID -> {
			JsonResultResponse<GeneDiseaseAnnotationDocument> paginationResult = getDiseaseAnnotationList(geneID, pagination);
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


	private JsonResultResponse<GeneDiseaseAnnotationDocument> getDiseaseAnnotationList(String geneID, Pagination pagination) {
		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);

		bool.filter(new TermQueryBuilder("category", "gene_disease_annotation"));
		bool2.should(new MatchQueryBuilder("subject.curie.keyword", geneID));

		// create histogram of select columns of unfiltered query

		HashMap<String, String> filterOptionMap = pagination.getFilterOptionMap();
		if (MapUtils.isNotEmpty(filterOptionMap)) {
			filterOptionMap.forEach((filterName, filterValue) -> {
				generateFilter(bool, filterName, filterValue);
			});
		}

		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();
		SearchResponse searchResponse = searchDAO.performQuery(
			bool, aggBuilders, null, geneDiseaseSearchHelper.getResponseFields(),
			pagination.getLimit(), pagination.getOffset(), hlb, null, false);

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
