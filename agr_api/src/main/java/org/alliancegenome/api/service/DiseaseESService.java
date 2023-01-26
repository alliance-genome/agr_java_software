package org.alliancegenome.api.service;

import static org.alliancegenome.cache.repository.helper.JsonResultResponse.DISTINCT_FIELD_VALUES;
import static org.alliancegenome.cache.repository.helper.JsonResultResponse.getEmptyInstance;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.entity.GeneDiseaseAnnotationDocument;
import org.alliancegenome.api.service.helper.GeneDiseaseSearchHelper;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.apache.commons.collections.MapUtils;
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
@RequestScoped
public class DiseaseESService {

	@Inject
	ObjectMapper mapper;
	private static SearchDAO searchDAO = new SearchDAO();

	private GeneDiseaseSearchHelper geneDiseaseSearchHelper = new GeneDiseaseSearchHelper();

	public JsonResultResponse<GeneDiseaseAnnotationDocument> getRibbonDiseaseAnnotationDocuments(List<String> geneIDs, String termID, Pagination pagination) {
		return getEmptyInstance();
	}

	public JsonResultResponse<GeneDiseaseAnnotationDocument> getRibbonDiseaseAnnotations(List<String> geneIDs, String termID, Pagination pagination) {

		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);

		bool.filter(new TermQueryBuilder("category", "gene_disease_annotation"));

		for (String geneId : geneIDs) {
			bool2.should(new MatchQueryBuilder("subject.curie.keyword", geneId));
		}

		// create histogram of select columns of unfiltered query
		Map<String, String> aggregationFields = new HashMap<>();
		aggregationFields.put("subject.taxon.name.keyword", "species");
		aggregationFields.put("diseaseRelation.name.keyword", "associationType");
		Map<String, List<String>> distinctFieldValueMap = addAggregations(bool, aggregationFields);

		HashMap<String, String> filterOptionMap = pagination.getFilterOptionMap();
		if (MapUtils.isNotEmpty(filterOptionMap)) {
			filterOptionMap.forEach((filterName, filterValue) -> {
				if (filterValue.contains("|")) {
					BoolQueryBuilder orClause = boolQuery();
					String[] elements = filterValue.split("\\|");
					Arrays.stream(elements).forEach(element -> orClause.should(QueryBuilders.termQuery(filterName, element)));
					bool.must(orClause);
				} else {
					if (filterName.contains("|")) {
						BoolQueryBuilder orClause = boolQuery();
						String[] elements = filterName.split("\\|");
						Arrays.stream(elements).forEach(element -> orClause.should(QueryBuilders.wildcardQuery(element, "*" + filterValue + "*")));
						bool.must(orClause);
					} else {
						bool.must(QueryBuilders.wildcardQuery(filterName, "*" + filterValue + "*"));
					}
				}
			});
		}

		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();
		SearchResponse searchResponse = searchDAO.performQuery(
			bool, aggBuilders, null, geneDiseaseSearchHelper.getResponseFields(),
			pagination.getLimit(), pagination.getOffset(), hlb, "diseaseAnnotation", false);


		log.info(geneIDs);
		log.info(termID);
		log.info(pagination);
		JsonResultResponse<GeneDiseaseAnnotationDocument> ret = new JsonResultResponse<>();
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		Map<String, Object> supplementalData = new LinkedHashMap<>();
		supplementalData.put(DISTINCT_FIELD_VALUES, distinctFieldValueMap);
		ret.setSupplementalData(supplementalData);

		List<GeneDiseaseAnnotationDocument> list = new ArrayList<>();
		ObjectMapper mapper2 = new ObjectMapper();
		JavaTimeModule module = new JavaTimeModule();
		mapper2.registerModule(module);
		mapper2.registerModule(new Jdk8Module());

//		mapper2.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
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

		//Aggregations aggs = searchResponseHistogram.getAggregations().get(fieldNameAgg);
		aggregationFields.forEach((field, colName) -> {
			String fieldNameAgg = field + "_agg";
			List<String> values = ((ParsedStringTerms) searchResponseHistogram.getAggregations().get(fieldNameAgg)).getBuckets().stream()
				.map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList());
			distinctFieldValueMap.put(colName, values);
		});
		return distinctFieldValueMap;
	}

}
