package org.alliancegenome.es.index.site.dao;

import java.io.IOException;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.ESDAO;
import org.apache.logging.log4j.*;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.elasticsearch.search.sort.SortOrder;

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

        SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
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

}
