package org.alliancegenome.variant_indexer.es;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ESDocumentInjector extends Thread {
    
    private BulkProcessor.Builder builder;
    private BulkProcessor bulkProcessor;
    private String indexName = "site_variant_index";
    //private boolean createIndex = false;
    private LinkedBlockingQueue<IndexRequest> queue = new LinkedBlockingQueue<>(10_000);
    
    private RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
            new HttpHost("54.91.239.235", 9200, "http"),
            new HttpHost("54.91.239.235", 9201, "http"),
            new HttpHost("54.91.239.235", 9202, "http"),
            new HttpHost("54.91.239.235", 9203, "http"),
            new HttpHost("54.91.239.235", 9204, "http"),
            new HttpHost("54.91.239.235", 9205, "http"),
            new HttpHost("54.91.239.235", 9206, "http"),
            new HttpHost("54.91.239.235", 9207, "http")
    ));
    
    public ESDocumentInjector(boolean createIndex) {
        //this.createIndex = createIndex;
        
        BulkProcessor.Listener listener = new BulkProcessor.Listener() { 
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                log.info("Size: " + request.requests().size() + " MB: " + request.estimatedSizeInBytes() + " Time: " + response.getTook() + " Bulk Requet Finished");
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                log.error("Bulk Requet Failure: " + failure.getMessage());
                log.error(request.toString());
                failure.printStackTrace();
                System.exit(-1);
            }
        };

        builder = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener);
        builder.setBulkActions(20000);
        builder.setConcurrentRequests(12);
        builder.setBulkSize(new ByteSizeValue(75, ByteSizeUnit.MB));
        builder.setFlushInterval(TimeValue.timeValueSeconds(60L));
        builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));

        bulkProcessor = builder.build();
        
        if(createIndex) {
        
            CreateIndexRequest indexRequest = new CreateIndexRequest(indexName);
            indexRequest.settings(Settings.builder() 
                    .put("index.number_of_shards", 8)
                    .put("index.refresh_interval", -1)
                    .put("index.number_of_replicas", 0)
                    );
    
            try {
                CreateIndexResponse createIndexResponse = client.indices().create(indexRequest, RequestOptions.DEFAULT);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public void run() {
        while(true) {
            IndexRequest req;
            try {
                req = queue.take();
                bulkProcessor.add(req);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void addDocument(String id, String json) {
        try {
            queue.offer(new IndexRequest(indexName).source(json, XContentType.JSON).id(id), 10, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
