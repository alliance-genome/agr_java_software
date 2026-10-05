package org.alliancegenome.api.es.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.es.util.EsClientFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filters;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregator.KeyedFilter;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchDAO extends ESDAO {

	public static final int COUNT_QUERY_BATCH_SIZE = 50;
	private static final String RELATED_COUNTS_AGGREGATION = "related_counts";

	/** Exact counts in input order, with one bounded search in flight at a time. */
	public List<Long> performCountQueries(List<QueryBuilder> queries) {
		List<Long> counts = new ArrayList<>(queries.size());
		for (int start = 0; start < queries.size(); start += COUNT_QUERY_BATCH_SIZE) {
			List<QueryBuilder> batch = queries.subList(start, Math.min(start + COUNT_QUERY_BATCH_SIZE, queries.size()));
			SearchRequest request = new SearchRequest(ConfigHelper.getEsIndex());
			request.allowPartialSearchResults(false);
			request.source(buildCountSearchSource(batch));
			SearchResponse response;
			try {
				response = executeCountSearch(request);
			} catch (IOException e) {
				throw new IllegalStateException("Unable to calculate related-data counts", e);
			}
			if (response == null || response.isTimedOut() || response.getFailedShards() > 0 || Boolean.TRUE.equals(response.isTerminatedEarly()) || response.getAggregations() == null) {
				throw new IllegalStateException("Incomplete related-data count response");
			}
			Filters filters = response.getAggregations().get(RELATED_COUNTS_AGGREGATION);
			if (filters == null) {
				throw new IllegalStateException("Missing related-data count aggregation");
			}
			for (int index = 0; index < batch.size(); index++) {
				Filters.Bucket bucket = filters.getBucketByKey(Integer.toString(index));
				if (bucket == null) {
					throw new IllegalStateException("Missing related-data count bucket");
				}
				counts.add(bucket.getDocCount());
			}
		}
		return counts;
	}

	protected SearchSourceBuilder buildCountSearchSource(List<QueryBuilder> queries) {
		if (queries.isEmpty() || queries.size() > COUNT_QUERY_BATCH_SIZE) {
			throw new IllegalArgumentException("Related-data count batch must contain 1 to " + COUNT_QUERY_BATCH_SIZE + " filters");
		}
		KeyedFilter[] filters = new KeyedFilter[queries.size()];
		BoolQueryBuilder matchingDocuments = QueryBuilders.boolQuery().minimumShouldMatch(1);
		for (int index = 0; index < queries.size(); index++) {
			QueryBuilder query = queries.get(index);
			filters[index] = new KeyedFilter(Integer.toString(index), query);
			matchingDocuments.should(query);
		}
		// Restrict collection to the union of the original predicates. Named filters
		// retain exact document counts, including documents matching multiple links.
		return new SearchSourceBuilder().size(0).trackTotalHits(false)
			.query(QueryBuilders.boolQuery().filter(matchingDocuments))
			.aggregation(AggregationBuilders.filters(RELATED_COUNTS_AGGREGATION, filters));
	}

	protected SearchResponse executeCountSearch(SearchRequest request) throws IOException {
		return EsClientFactory.getDefaultEsClient().search(request, RequestOptions.DEFAULT);
	}

	public Long performCountQuery(QueryBuilder query) {
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		searchSourceBuilder.query(query);
		searchSourceBuilder.size(0);
		searchSourceBuilder.trackTotalHits(true);

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
			return 0L;
		}

	}

	public SearchResponse performQuery(QueryBuilder query, List<AggregationBuilder> aggBuilders, QueryRescorerBuilder rescorerBuilder, List<String> responseFields, int limit, int offset, HighlightBuilder highlighter, LinkedHashMap<String, SortOrder> sorts, Boolean debug) {
		return performQuery(query, aggBuilders, rescorerBuilder, responseFields, limit, offset, highlighter, sorts, null, debug);
	}

	public SearchResponse performQuery(QueryBuilder query, List<AggregationBuilder> aggBuilders, QueryRescorerBuilder rescorerBuilder, List<String> responseFields, int limit, int offset, HighlightBuilder highlighter, LinkedHashMap<String, SortOrder> sorts, Map<String, Boolean> fieldSorter,
		Boolean debug) {
		return performQuery(query, aggBuilders, rescorerBuilder, responseFields, limit, offset, highlighter, sorts, fieldSorter, null, debug);
	}

	public SearchResponse performQuery(QueryBuilder query, List<AggregationBuilder> aggBuilders, QueryRescorerBuilder rescorerBuilder, List<String> responseFields, int limit, int offset, HighlightBuilder highlighter, LinkedHashMap<String, SortOrder> sorts, Map<String, Boolean> fieldSorter,
		List<String> sourceExcludes, Boolean debug) {

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		String[] excludes = (sourceExcludes == null || sourceExcludes.isEmpty()) ? null : sourceExcludes.toArray(new String[0]);
		searchSourceBuilder.fetchSource(responseFields.toArray(new String[responseFields.size()]), excludes);

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

		if (fieldSorter != null) {
			fieldSorter.forEach((key, value) -> {
				FieldSortBuilder fieldSortBuilder = new FieldSortBuilder(key);
				fieldSortBuilder.missing(value ? "_last" : "_first");
				searchSourceBuilder.sort(fieldSortBuilder);
			});
		}

		if (sorts != null) {
			for (Entry<String, SortOrder> entry : sorts.entrySet()) {
				searchSourceBuilder.sort(entry.getKey(), entry.getValue());
			}
		}

		searchSourceBuilder.highlighter(highlighter);

		for (AggregationBuilder aggBuilder : aggBuilders) {
			searchSourceBuilder.aggregation(aggBuilder);
		}

		if (debug != null && debug) {
			log.info("searchSourceBuilder: " + searchSourceBuilder);
		} else {
			log.debug("searchSourceBuilder: " + searchSourceBuilder);
		}

		SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
		searchRequest.source(searchSourceBuilder);

		if (debug != null && debug) {
			log.info("Request: " + searchRequest);
		} else {
			log.debug("Request: " + searchRequest);
		}

		SearchResponse response = null;

		try {
			response = EsClientFactory.getDefaultEsClient().search(searchRequest, EsClientFactory.LARGE_RESPONSE_REQUEST_OPTIONS);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return response;
	}

}
