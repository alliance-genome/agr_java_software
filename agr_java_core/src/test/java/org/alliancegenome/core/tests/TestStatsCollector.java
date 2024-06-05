package org.alliancegenome.core.tests;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.es.util.EsClientFactory;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;


public class TestStatsCollector {

   public static void main(String[] args) throws Exception {
	  new TestStatsCollector();
   }

   private TestStatsCollector() throws Exception {

	  System.out.println("We are here");
	  ConfigHelper.init();
	  System.out.println("We are here");
	  SearchDAO dao = new SearchDAO();
	  System.out.println("We are here");


	  SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

	  MatchQueryBuilder builder = QueryBuilders.matchQuery("category", "gene_disease_annotation");

	  searchSourceBuilder.query(builder);
	  searchSourceBuilder.from(0);
	  searchSourceBuilder.size(10000);

	  SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
	  searchRequest.source(searchSourceBuilder);

	  //Long count = dao.performQuery(searchSourceBuilder, );
	  RestHighLevelClient client = EsClientFactory.getMustCloseSearchClient();

	  SearchResponse resp = client.search(searchRequest, EsClientFactory.LARGE_RESPONSE_REQUEST_OPTIONS);

	  SummaryStatistics stats = new SummaryStatistics();

	  for (SearchHit hit: resp.getHits()) {
		 int len = hit.getSourceAsString().length();
		 stats.addValue(len);
	  }

	  System.out.println(stats);

	  // long sum = 1_467_274_439;
	  client.close();
   }
}