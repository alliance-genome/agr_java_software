package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.api.service.helper.GeneDiseaseSearchHelper;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import lombok.extern.jbosslog.JBossLog;

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

		for(String geneId: geneIDs) {
			bool2.should().add(new MatchQueryBuilder("subject.curie", geneId));
		}

		HighlightBuilder hlb = new HighlightBuilder();
		
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		
		SearchResponse searchResponse = searchDAO.performQuery(
			bool, aggBuilders, null, geneDiseaseSearchHelper.getResponseFields(),
			pagination.getLimit(), pagination.getOffset(), hlb, null, false);
		
		log.info(geneIDs);
		log.info(termID);
		log.info(pagination);
		JsonResultResponse<JSONObject> ret = new JsonResultResponse<>();
		ret.setTotal((int)searchResponse.getHits().getTotalHits().value);

		JSONParser parser = new JSONParser();
		ArrayList<JSONObject> list = new ArrayList<>();
		for(SearchHit searchHit: searchResponse.getHits().getHits()) {
			try {
				JSONObject object = (JSONObject)parser.parse(searchHit.getSourceAsString());
				object.put("id", searchHit.getId());
				list.add(object);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		ret.setResults(list);
	
		return ret;
	}

}
