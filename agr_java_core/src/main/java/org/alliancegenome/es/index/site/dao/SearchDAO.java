package org.alliancegenome.es.index.site.dao;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.ESDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.io.IOException;
import java.util.List;

@SuppressWarnings("serial")
public class SearchDAO extends ESDAO {

    private final Logger log = LogManager.getLogger(getClass());

    public Long performCountQuery(QueryBuilder query) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        searchSourceBuilder.query(query);
        searchSourceBuilder.size(0);

        SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
        searchRequest.source(searchSourceBuilder);

        SearchResponse response = null;

        try {
            response = searchClient.search(searchRequest, RequestOptions.DEFAULT);
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
            String sort, Boolean debug) {


        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();


        searchSourceBuilder.fetchSource(responseFields.toArray(new String[responseFields.size()]), null);

        if (debug) {
            searchSourceBuilder.explain(true);
        }

        if (rescorerBuilder != null) {
            searchSourceBuilder.addRescorer(rescorerBuilder);
        }

        searchSourceBuilder.query(query);
        searchSourceBuilder.size(limit);
        searchSourceBuilder.from(offset);
        searchSourceBuilder.trackTotalHits(true);

        if(sort != null && sort.equals("alphabetical")) {
            searchSourceBuilder.sort("name.keyword", SortOrder.ASC);
        }
        searchSourceBuilder.highlighter(highlighter);


        for(AggregationBuilder aggBuilder: aggBuilders) {
            searchSourceBuilder.aggregation(aggBuilder);
        }

        log.debug(searchSourceBuilder);

        SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex(),  "variant_index_1616176744699");
        //variant_index_1615903310632
        searchRequest.source(searchSourceBuilder);
//        searchRequest.preference("p_" + query);

        SearchResponse response = null;

        try {
            response = searchClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }
    public SearchResponse performQuery(QueryBuilder query) {


        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        searchSourceBuilder.query(query);

        //   SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex(), "variant_index_1615694790559");
        SearchRequest searchRequest = new SearchRequest( "variant_index_1616176744699");


        searchRequest.source(searchSourceBuilder);
//        searchRequest.preference("p_" + query);

        SearchResponse response = null;

        try {
            response = searchClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }


    public SearchResponse query(String q, String category) {

        BoolQueryBuilder query = new BoolQueryBuilder();
        query.must(disMaxQueryBuilder(q, category))
                .filter(QueryBuilders.termQuery("category.keyword", "allele"));
        return performQuery(query);

    }


    public DisMaxQueryBuilder disMaxQueryBuilder(String q, String category){
        DisMaxQueryBuilder qb=new DisMaxQueryBuilder();
        return qb.add(QueryBuilders.multiMatchQuery(q,"name",  "symbol","genes","name_key","id"));
    }
}
