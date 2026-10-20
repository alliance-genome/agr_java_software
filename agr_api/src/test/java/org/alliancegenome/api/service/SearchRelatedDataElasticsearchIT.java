package org.alliancegenome.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.alliancegenome.api.es.dao.SearchDAO;
import org.alliancegenome.api.es.search.Category;
import org.alliancegenome.api.es.search.RelatedDataLink;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

/** Opt-in regression against a local, disposable Elasticsearch 7.17 instance. */
public class SearchRelatedDataElasticsearchIT {

	private static RestHighLevelClient client;
	private static String indexName;
	private static boolean indexCreated;

	@BeforeClass
	public static void seedMultiShardIndex() throws IOException {
		String url = System.getProperty("relatedDataTestUrl");
		Assume.assumeTrue("Set relatedDataTestUrl to a local disposable Elasticsearch instance", url != null);
		URI uri = URI.create(url);
		assertTrue("Integration fixture must use a loopback host", List.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost()));
		client = new RestHighLevelClient(RestClient.builder(HttpHost.create(url)));
		indexName = "agr-related-count-test-" + UUID.randomUUID();
		CreateIndexRequest create = new CreateIndexRequest(indexName).settings(Map.of("number_of_shards", 3, "number_of_replicas", 0));
		Map<String, Object> linkedField = Map.of("type", "text", "fields", Map.of("keyword", Map.of("type", "keyword")));
		create.mapping(Map.of("properties", Map.of("category", Map.of("type", "keyword"), "genes", linkedField, "models", linkedField)));
		client.indices().create(create, RequestOptions.DEFAULT);
		indexCreated = true;
		BulkRequest bulk = new BulkRequest();
		for (int index = 0; index < 10005; index++) {
			Map<String, Object> document = new LinkedHashMap<>(Map.of("category", Category.DISEASE.getName(), "genes", List.of("alpha")));
			if (index == 0) {
				document.put("models", List.of("model-alpha"));
			}
			bulk.add(new IndexRequest(indexName).id("disease-" + index).source(document));
			if (bulk.numberOfActions() == 1000) {
				assertFalse(client.bulk(bulk, RequestOptions.DEFAULT).hasFailures());
				bulk = new BulkRequest();
			}
		}
		bulk.add(new IndexRequest(indexName).id("overlap").source(Map.of("category", Category.DISEASE.getName(), "genes", List.of("alpha", "alpha", "beta"))));
		bulk.add(new IndexRequest(indexName).id("other").source(Map.of("category", Category.DISEASE.getName(), "genes", List.of("gamma"))));
		bulk.add(new IndexRequest(indexName).id("missing").source(Map.of("category", Category.DISEASE.getName())));
		bulk.add(new IndexRequest(indexName).id("allele").source(Map.of("category", Category.ALLELE.getName(), "genes", List.of("alpha"), "models", List.of("model-alpha"))));
		bulk.add(new IndexRequest(indexName).id("gene").source(Map.of("category", Category.GENE.getName(), "models", List.of("model-alpha"))));
		bulk.add(new IndexRequest(indexName).id("model").source(Map.of("category", Category.MODEL.getName(), "genes", List.of("alpha"))));
		assertFalse(client.bulk(bulk, RequestOptions.DEFAULT).hasFailures());
		client.indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT);
	}

	@AfterClass
	public static void removeOnlyTheFixtureIndex() throws IOException {
		if (client != null) {
			try {
				if (indexCreated) {
					client.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
				}
			} finally {
				client.close();
			}
		}
	}

	@Test
	public void filtersRetainExactOriginalCountsAcrossShardsAndOverlappingMultiValuedFields() throws IOException {
		List<QueryBuilder> predicates = List.of(
			countQuery(Category.DISEASE, "alpha"), countQuery(Category.DISEASE, "beta"), countQuery(Category.DISEASE, "unknown"),
			countQuery(Category.ALLELE, "alpha"), QueryBuilders.boolQuery().filter(QueryBuilders.termQuery("category", Category.DISEASE.getName()))
				.mustNot(QueryBuilders.termQuery("genes.keyword", "alpha")));
		List<Long> expected = List.of(10006L, 1L, 0L, 1L, 2L);
		for (int index = 0; index < predicates.size(); index++) {
			SearchRequest original = new SearchRequest(indexName).source(new SearchSourceBuilder().query(predicates.get(index)).size(0).trackTotalHits(true));
			assertEquals(expected.get(index).longValue(), client.search(original, RequestOptions.DEFAULT).getHits().getTotalHits().value);
		}
		List<QueryBuilder> requests = new ArrayList<>();
		for (int index = 0; index < 250; index++) {
			requests.add(predicates.get(index % predicates.size()));
		}
		LocalDAO dao = new LocalDAO();

		List<Long> counts = dao.performCountQueries(requests);

		assertEquals(5, dao.searches);
		for (int index = 0; index < counts.size(); index++) {
			assertEquals(expected.get(index % expected.size()), counts.get(index));
		}
	}

	@Test
	public void fiftyGeneResultsUseFiveSearchesWithCompatibleLinksAndNoStoredMetadata() {
		LocalDAO dao = new LocalDAO();
		List<Map<String, Object>> results = new ArrayList<>();
		for (int index = 0; index < 50; index++) {
			String name = index == 0 ? "alpha" : index == 1 ? "beta" : "absent-" + index;
			results.add(new LinkedHashMap<>(Map.of("category", Category.GENE.getName(), "nameKey", name)));
		}

		new SearchService(dao).addRelatedDataLinks(results);

		assertEquals(5, dao.searches);
		List<RelatedDataLink> alpha = links(results.getFirst());
		assertEquals(List.of(Category.DISEASE.getName(), Category.ALLELE.getName(), Category.MODEL.getName()), alpha.stream().map(RelatedDataLink::getCategory).toList());
		assertEquals(List.of(10006L, 1L, 1L), alpha.stream().map(RelatedDataLink::getCount).toList());
		assertEquals(1, links(results.get(1)).size());
		assertEquals(Long.valueOf(1), links(results.get(1)).getFirst().getCount());
		assertTrue(results.subList(2, results.size()).stream().allMatch(result -> links(result).isEmpty()));
		Map<String, Object> model = new LinkedHashMap<>(Map.of("category", Category.MODEL.getName(), "nameKey", "model-alpha"));
		new SearchService(dao).addRelatedDataLinks(model);
		assertEquals(List.of(Category.GENE.getName(), Category.ALLELE.getName(), Category.DISEASE.getName()), links(model).stream().map(RelatedDataLink::getCategory).toList());
		assertEquals(List.of(1L, 1L, 1L), links(model).stream().map(RelatedDataLink::getCount).toList());
	}

	private static QueryBuilder countQuery(Category category, String gene) {
		return QueryBuilders.boolQuery().filter(QueryBuilders.termQuery("category", category.getName())).filter(QueryBuilders.termQuery("genes.keyword", gene));
	}

	@SuppressWarnings("unchecked")
	private static List<RelatedDataLink> links(Map<String, Object> result) {
		return (List<RelatedDataLink>) result.get("relatedData");
	}

	private static class LocalDAO extends SearchDAO {
		private int searches;

		@Override
		protected SearchResponse executeCountSearch(SearchRequest request) throws IOException {
			searches++;
			request.indices(indexName);
			return client.search(request, RequestOptions.DEFAULT);
		}
	}
}
