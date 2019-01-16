package org.alliancegenome.es.index;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

public class ESDAO {

    private Log log = LogFactory.getLog(getClass());

    protected static PreBuiltTransportClient searchClient = null; // Make sure to only have 1 of these clients to save on resources

    public ESDAO() {
        init();
    }

    public void init() {
        if(searchClient == null) {
            searchClient = new PreBuiltTransportClient(Settings.EMPTY);
            try {
                if(ConfigHelper.getEsHost().contains(",")) {
                    String[] hosts = ConfigHelper.getEsHost().split(",");
                    for(String host: hosts) {
                        searchClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), ConfigHelper.getEsPort()));
                    }
                } else {
                    searchClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ConfigHelper.getEsHost()), ConfigHelper.getEsPort()));
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        log.info("Closing Down ES Client");
        searchClient.close();
    }

    // This method is responsible to make sure that the data index is created
    protected void checkIndex(String index) {
        IndicesExistsRequest request = new IndicesExistsRequest(index);
        try {
            IndicesExistsResponse res = searchClient.admin().indices().exists(request).get();
            if(!res.isExists()) {
                log.info(index + " not found creating it");
                Settings settings = Settings.builder()
                        .put("index.number_of_replicas", 0)
                        .build();
                searchClient.admin().indices().create(new CreateIndexRequest(index).settings(settings)).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    protected SearchApiResponse getSearchResult(Pagination pagination, SearchRequestBuilder searchRequestBuilder) {
        searchRequestBuilder.setSize(pagination.getLimit());
        int fromIndex = pagination.getIndexOfFirstElement();
        searchRequestBuilder.setFrom(fromIndex);

        org.elasticsearch.action.search.SearchResponse response = searchRequestBuilder.execute().actionGet();
        SearchApiResponse result = new SearchApiResponse();

        result.setTotal(response.getHits().totalHits);
        result.setResults(formatResults(response));
        return result;
    }

    protected ArrayList<Map<String, Object>> formatResults(org.elasticsearch.action.search.SearchResponse response) {

        ArrayList<Map<String, Object>> ret = new ArrayList<>();

        for (SearchHit hit : response.getHits()) {
            hit.getSourceAsMap().put("id", hit.getId());
            //hit.getSource().put("explain", hit.getExplanation());
            ret.add(hit.getSourceAsMap());
        }
        return ret;
    }

    protected SortOrder getAscending(Boolean ascending) {
        return ascending ? SortOrder.ASC : SortOrder.DESC;
    }




}
