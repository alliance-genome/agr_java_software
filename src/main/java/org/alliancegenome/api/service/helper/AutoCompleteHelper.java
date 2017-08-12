package org.alliancegenome.api.service.helper;

import java.util.ArrayList;
import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

@RequestScoped
public class AutoCompleteHelper {

	public QueryBuilder buildQuery(String q, String category) {
		
		BoolQueryBuilder bool = new BoolQueryBuilder();

		bool.must(new MatchQueryBuilder("name_key.autocomplete", q).operator(Operator.AND).boost(3));
		bool.must(new MatchQueryBuilder("name.raw", q).operator(Operator.AND).boost(2));
		bool.must(new MatchQueryBuilder("name.autocomplete", q).operator(Operator.AND).boost(1));
		bool.must(new MatchQueryBuilder("synonyms.raw", q).operator(Operator.AND).boost(2));
		bool.must(new MatchQueryBuilder("synonyms.autocomplete", q).operator(Operator.AND).boost(1));

		return bool;
	}

	public ArrayList<Map<String, Object>> formatResults(SearchResponse res) {
		ArrayList<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
		
		for(SearchHit hit: res.getHits()) {
			ret.add(hit.getSource());
		}
		return ret;
	}

}
