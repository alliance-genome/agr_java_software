package org.alliancegenome.es.index.site.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.ESDAO;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class AutoCompleteDAO extends ESDAO {

	//private Logger log = Logger.getLogger(getClass());

	private List<String> response_fields = new ArrayList<String>() {
		{
			add("name_key"); add("name"); add("symbol");
			add("primaryKey"); add("category"); add("go_type");
		}
	};

	public SearchResponse performQuery(QueryBuilder query) {

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(query);

		SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
		searchRequest.source(searchSourceBuilder);

		searchSourceBuilder.fetchSource(response_fields.toArray(new String[response_fields.size()]), null);
		searchSourceBuilder.query(query);

		SearchResponse response = null;

		try {
			response = searchClient.search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return response;
	}

}
