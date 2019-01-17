package org.alliancegenome.es.index.site.dao;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.ESDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.sort.SortBuilders;

public class GeneDAO extends ESDAO {

    private Log log = LogFactory.getLog(getClass());

    public Map<String, Object> getById(String id) {
        try {
            GetRequest request = new GetRequest();
            request.id(id);
            request.type("gene");
            request.index(ConfigHelper.getEsIndex());
            GetResponse res = searchClient.get(request).get();
            //log.info(res);
            return res.getSource();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, Object> getGeneBySecondary(String id) {
        SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch();

        //searchRequestBuilder.setExplain(true);
        searchRequestBuilder.setIndices(ConfigHelper.getEsIndex());

        // match on secondary IDs
        MatchQueryBuilder query = QueryBuilders.matchQuery("secondaryIds", id);
        searchRequestBuilder.setQuery(query);
        org.elasticsearch.action.search.SearchResponse response = searchRequestBuilder.execute().actionGet();
        long total = response.getHits().totalHits;
        if (total > 0)
            return formatResults(response).get(0);
        else
            return null;
    }

    public SearchApiResponse getAllelesByGene(String geneId, Pagination pagination) {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder(geneId);
        if (pagination != null) {
            searchRequestBuilder.setSize(pagination.getLimit());
            int fromIndex = pagination.getIndexOfFirstElement();
            searchRequestBuilder.setFrom(fromIndex);
        }
        org.elasticsearch.action.search.SearchResponse response = searchRequestBuilder.execute().actionGet();
        SearchApiResponse result = new SearchApiResponse();

        result.setTotal(response.getHits().totalHits);
        result.setResults(formatResults(response));
        return result;
    }

    private SearchRequestBuilder getSearchRequestBuilder(String geneID) {
        SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch();

        //searchRequestBuilder.setExplain(true);
        searchRequestBuilder.setIndices(ConfigHelper.getEsIndex());

        TermQueryBuilder builder = QueryBuilders.termQuery("geneDocument.primaryId", geneID);
        BoolQueryBuilder query = QueryBuilders.boolQuery().must(builder);
        MultiMatchQueryBuilder multiBuilder = QueryBuilders.multiMatchQuery("allele", "category")
                .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX);
        query.must(multiBuilder);
        searchRequestBuilder.addSort(SortBuilders.fieldSort("symbol.sort"));
        searchRequestBuilder.setQuery(query);
        log.debug(searchRequestBuilder);
        return searchRequestBuilder;
    }

}