package org.alliancegenome.es.index.site.dao;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.ESDAO;
import org.alliancegenome.es.util.EsClientFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.sort.SortOrder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchDAO extends ESDAO {


	public Long performCountQuery(QueryBuilder query) {
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		searchSourceBuilder.query(query);
		searchSourceBuilder.size(0);

		SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
		searchRequest.source(searchSourceBuilder);

		SearchResponse response = null;

		try {
			response = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}


		if (response != null && response.getHits() != null) {
			return response.getHits().getTotalHits().value;
		} else {
			return 0l;
		}

	}

	public SearchResponse performQuery(QueryBuilder query,
			List<AggregationBuilder> aggBuilders,
			QueryRescorerBuilder rescorerBuilder,
			List<String> responseFields,
			int limit, int offset,
			HighlightBuilder highlighter,
			HashMap<String, SortOrder> sorts, Boolean debug) {


		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		searchSourceBuilder.fetchSource(responseFields.toArray(new String[responseFields.size()]), null);

		if (debug != null && debug) {
			searchSourceBuilder.explain(true);
		}

		if (rescorerBuilder != null) {
			searchSourceBuilder.addRescorer(rescorerBuilder);
		}

		searchSourceBuilder.query(query);
		searchSourceBuilder.size(limit);
		searchSourceBuilder.from(offset);
		searchSourceBuilder.trackTotalHits(true);

		if(sorts != null) {
			for(Entry<String, SortOrder> entry: sorts.entrySet()) {
				searchSourceBuilder.sort(entry.getKey(), entry.getValue());
			}
		}

		searchSourceBuilder.highlighter(highlighter);

		for(AggregationBuilder aggBuilder: aggBuilders) {
			searchSourceBuilder.aggregation(aggBuilder);
		}

		if(debug != null && debug) {
			log.info("searchSourceBuilder: " + searchSourceBuilder);
		} else {
			log.debug("searchSourceBuilder: " + searchSourceBuilder);
		}

		SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
		searchRequest.source(searchSourceBuilder);
		// This request cache doesn't work 07/07/2021
		//searchRequest.requestCache(true);

		if(debug != null && debug) {
			log.info("Request: " + searchRequest);
		} else {
			log.debug("Request: " + searchRequest);
		}

		SearchResponse response = null;

		try {
			response = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return response;
	}

}
