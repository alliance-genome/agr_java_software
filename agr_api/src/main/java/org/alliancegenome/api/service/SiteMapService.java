package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.io.IOException;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.EsClientFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class SiteMapService {

	public SearchResponse getAccession(String category, String keyField, String keyValue) {

		BoolQueryBuilder bool = boolQuery();
		bool.filter(new TermQueryBuilder("category", category));
		bool.must(new TermQueryBuilder(keyField, keyValue));
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(bool);

		SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
		searchRequest.source(searchSourceBuilder);

		try {
			return EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;

	}
	
	public SearchResponse getSiteMap(String siteMapId) {
		BoolQueryBuilder bool = boolQuery();
		bool.filter(new TermQueryBuilder("category", "sitemapid"));
		bool.must(new TermQueryBuilder("siteMapId", siteMapId));
		
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(bool);
		searchSourceBuilder.size(1000);

		SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
		searchRequest.source(searchSourceBuilder);

		try {
			return EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public SearchResponse getFullSiteMap() {
		BoolQueryBuilder bool = boolQuery();
		bool.filter(new TermQueryBuilder("category", "sitemapid"));
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.fetchSource(new String[] {"siteMapId"}, null);
		searchSourceBuilder.query(bool);
		searchSourceBuilder.size(1000);

		SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
		searchRequest.source(searchSourceBuilder);

		try {
			return EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
