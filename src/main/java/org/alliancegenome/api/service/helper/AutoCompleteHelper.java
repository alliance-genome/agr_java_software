package org.alliancegenome.api.service.helper;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.jboss.logging.Logger;

import javax.enterprise.context.RequestScoped;
import java.util.ArrayList;
import java.util.Map;

@RequestScoped
public class AutoCompleteHelper {

	private Logger log = Logger.getLogger(getClass());

	public QueryBuilder buildQuery(String q, String category) {
		
		BoolQueryBuilder bool = new BoolQueryBuilder();

		MultiMatchQueryBuilder multi = QueryBuilders.multiMatchQuery(q);
		multi.field("symbol",5.0F);
		multi.field("name_key.autocomplete",3.0F);
		multi.field("name.keyword", 2.0F);
		multi.field("name.autocomplete");
		multi.field("synonyms.keyword", 2.0F);
		multi.field("synonyms.autocomplete");

		bool.must(multi);

		return bool;
	}

	public ArrayList<Map<String, Object>> formatResults(SearchResponse res) {
		ArrayList<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
		
		for(SearchHit hit: res.getHits()) {
			String category = (String) hit.getSource().get("category");

			//this comes over from the Python code, use symbol for genes,
			//seems like maybe it could also use name_key for everyone...
			if (StringUtils.equals(category,"gene")) {
				hit.getSource().put("name", hit.getSource().get("symbol"));
			}
			ret.add(hit.getSource());
		}
		return ret;
	}

}
