package org.alliancegenome.api.service.helper;

import java.util.ArrayList;
import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

@RequestScoped
public class AutoCompleteHelper {

	public QueryBuilder buildQuery(String q, String category) {
		
		BoolQueryBuilder bool = new BoolQueryBuilder();

		bool.must(new MatchQueryBuilder("name_key.autocomplete", q));
		
		if(!(category == null || category.equals("") || category.length() == 0) && category.equals("gene")) {
			MatchQueryBuilder categoryMatch = new MatchQueryBuilder("category", "gene");
			categoryMatch.boost(2);
			bool.should(categoryMatch);
		}
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
