package org.alliancegenome.api.es.dao;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.api.es.dao.ESDAO;
import org.alliancegenome.es.util.EsClientFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public class AutoCompleteDAO extends ESDAO {

	private List<String> responseFields = Arrays.asList(
		"name", "symbol", "curie", "primaryKey", "category", "go_type", "nameKey"
	);

	public SearchResponse performQuery(QueryBuilder query) {

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(query);

		SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
		searchRequest.source(searchSourceBuilder);

		searchSourceBuilder.fetchSource(responseFields.toArray(new String[responseFields.size()]), null);
		searchSourceBuilder.query(query);

		SearchResponse response = null;

		try {
			response = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return response;
	}

}
