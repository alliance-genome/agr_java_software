package org.alliancegenome.api.es.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchResponse.Clusters;
import org.elasticsearch.action.search.SearchResponseSections;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.elasticsearch.search.aggregations.bucket.filter.FiltersAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilters;
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilters.InternalBucket;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;

public class SearchDAOCountQueriesTest {

	@Test
	public void fiftyGeneResultsRequireFiveSearchesAndRetainExactCountsInInputOrder() {
		CapturingDAO dao = new CapturingDAO();
		List<QueryBuilder> queries = queries(250);

		List<Long> counts = dao.performCountQueries(queries);

		assertEquals(5, dao.requests.size());
		assertEquals(250, counts.size());
		for (int index = 0; index < counts.size(); index++) {
			assertEquals(Long.valueOf(20000L + index), counts.get(index));
		}
		for (int batch = 0; batch < dao.requests.size(); batch++) {
			SearchRequest request = dao.requests.get(batch);
			assertEquals(Boolean.FALSE, request.allowPartialSearchResults());
			assertEquals(0, request.source().size());
			assertEquals(Integer.valueOf(-1), request.source().trackTotalHitsUpTo());
			FiltersAggregationBuilder aggregation = aggregation(request.source());
			assertEquals(50, aggregation.filters().size());
			BoolQueryBuilder root = (BoolQueryBuilder) request.source().query();
			BoolQueryBuilder union = (BoolQueryBuilder) root.filter().getFirst();
			assertEquals("1", union.minimumShouldMatch());
			for (var filter : aggregation.filters()) {
				int index = Integer.parseInt(filter.key());
				assertEquals(queries.get(batch * 50 + index), filter.filter());
				assertEquals(filter.filter(), union.should().get(index));
			}
		}
	}

	@Test
	public void processesTheFinalShortBatchAndKeepsTrueZeroCounts() {
		CapturingDAO dao = new CapturingDAO();
		dao.zeroCounts = true;

		List<Long> counts = dao.performCountQueries(queries(51));

		assertEquals(2, dao.requests.size());
		assertEquals(1, aggregation(dao.requests.get(1).source()).filters().size());
		assertEquals(51, counts.size());
		assertTrue(counts.stream().allMatch(count -> count == 0));
	}

	@Test
	public void noCountFiltersMeansNoElasticsearchRequest() {
		CapturingDAO dao = new CapturingDAO();

		assertTrue(dao.performCountQueries(List.of()).isEmpty());
		assertTrue(dao.requests.isEmpty());
	}

	@Test
	public void rejectsInvalidBatchSizes() {
		CapturingDAO dao = new CapturingDAO();

		assertThrows(IllegalArgumentException.class, () -> dao.buildCountSearchSource(List.of()));
		assertThrows(IllegalArgumentException.class, () -> dao.buildCountSearchSource(queries(51)));
	}

	@Test
	public void ioFailureInALaterBatchDoesNotReturnPartialCounts() {
		CapturingDAO dao = new CapturingDAO();
		dao.failOnRequest = 2;

		IllegalStateException error = assertThrows(IllegalStateException.class, () -> dao.performCountQueries(queries(51)));

		assertTrue(error.getCause() instanceof IOException);
		assertEquals(2, dao.requests.size());
	}

	@Test
	public void incompleteResponsesNeverBecomeZeroCounts() {
		for (String failure : List.of("timeout", "shard", "early", "aggregation", "bucket", "null")) {
			CapturingDAO dao = new CapturingDAO();
			dao.responseFailure = failure;

			assertThrows(failure, IllegalStateException.class, () -> dao.performCountQueries(queries(1)));
		}
	}

	private static List<QueryBuilder> queries(int size) {
		List<QueryBuilder> queries = new ArrayList<>();
		for (int index = 0; index < size; index++) {
			queries.add(QueryBuilders.boolQuery().filter(QueryBuilders.termQuery("category", "gene_search_result"))
				.filter(QueryBuilders.termQuery("models.keyword", "model-" + index)));
		}
		return queries;
	}

	private static FiltersAggregationBuilder aggregation(SearchSourceBuilder source) {
		return (FiltersAggregationBuilder) source.aggregations().getAggregatorFactories().iterator().next();
	}

	private static class CapturingDAO extends SearchDAO {
		private final List<SearchRequest> requests = new ArrayList<>();
		private int failOnRequest = -1;
		private String responseFailure = "";
		private boolean zeroCounts;

		@Override
		protected SearchResponse executeCountSearch(SearchRequest request) throws IOException {
			requests.add(request);
			if (requests.size() == failOnRequest) {
				throw new IOException("Synthetic count failure");
			}
			if ("null".equals(responseFailure)) {
				return null;
			}
			List<InternalBucket> buckets = new ArrayList<>();
			for (var filter : aggregation(request.source()).filters()) {
				long count = zeroCounts ? 0 : 20000L + (requests.size() - 1) * COUNT_QUERY_BATCH_SIZE + Integer.parseInt(filter.key());
				buckets.add(new InternalBucket(filter.key(), count, InternalAggregations.EMPTY, true));
			}
			// Return shuffled buckets: input ordering must come from their keys.
			java.util.Collections.reverse(buckets);
			if ("bucket".equals(responseFailure)) {
				buckets.clear();
			}
			InternalAggregations aggregations = "aggregation".equals(responseFailure) ? InternalAggregations.EMPTY
				: InternalAggregations.from(List.of(new InternalFilters("related_counts", buckets, true, Map.of())));
			SearchHits hits = new SearchHits(new SearchHit[0], null, Float.NaN);
			SearchResponseSections sections = new SearchResponseSections(hits, aggregations, null,
				"timeout".equals(responseFailure), "early".equals(responseFailure) ? Boolean.TRUE : null, null, 1);
			ShardSearchFailure[] failures = "shard".equals(responseFailure)
				? new ShardSearchFailure[] { new ShardSearchFailure(new IllegalStateException("Synthetic shard failure")) } : ShardSearchFailure.EMPTY_ARRAY;
			return new SearchResponse(sections, null, 1, failures.length == 0 ? 1 : 0, 0, 1L, failures, Clusters.EMPTY);
		}
	}
}
