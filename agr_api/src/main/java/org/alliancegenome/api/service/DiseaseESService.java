package org.alliancegenome.api.service;

import lombok.extern.jbosslog.JBossLog;
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
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import javax.enterprise.context.RequestScoped;
import java.util.*;
import java.util.stream.Collectors;

import static org.alliancegenome.cache.repository.helper.JsonResultResponse.DISTINCT_FIELD_VALUES;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

@JBossLog
@RequestScoped
public class DiseaseESService {

	private static SearchDAO searchDAO = new SearchDAO();

	private GeneDiseaseSearchHelper geneDiseaseSearchHelper = new GeneDiseaseSearchHelper();

	public JsonResultResponse<JSONObject> getRibbonDiseaseAnnotations(List<String> geneIDs, String termID, Pagination pagination) {

		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);

		bool.filter(new TermQueryBuilder("category", "gene_disease_annotation"));

		for (String geneId : geneIDs) {
			bool2.should().add(new MatchQueryBuilder("subject.curie", geneId));
		}

		// create histogram of select columns of unfiltered query
		Map<String, String> aggregationFields = new HashMap<>();
		aggregationFields.put("subject.taxon.name.keyword", "species");
		aggregationFields.put("diseaseRelation.name.keyword", "associationType");
		Map<String, List<String>> distinctFieldValueMap = addAggregations(bool, aggregationFields);

		HashMap<String, String> filterOptionMap = pagination.getFilterOptionMap();
		if (MapUtils.isNotEmpty(filterOptionMap)) {
			filterOptionMap.forEach((filterName, filterValue) -> {
				bool.must(QueryBuilders.wildcardQuery(filterName, "*" + filterValue + "*"));
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
		JsonResultResponse<JSONObject> ret = new JsonResultResponse<>();
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		Map<String, Object> supplementalData = new LinkedHashMap<>();
		supplementalData.put(DISTINCT_FIELD_VALUES, distinctFieldValueMap);
		ret.setSupplementalData(supplementalData);

		JSONParser parser = new JSONParser();
		ArrayList<JSONObject> list = new ArrayList<>();
		for (SearchHit searchHit : searchResponse.getHits().getHits()) {
			try {
				JSONObject object = (JSONObject) parser.parse(searchHit.getSourceAsString());
				object.put("id", searchHit.getId());
				list.add(object);
			} catch (ParseException e) {
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
