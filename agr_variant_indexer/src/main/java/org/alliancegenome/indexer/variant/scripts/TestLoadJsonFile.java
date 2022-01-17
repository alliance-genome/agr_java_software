package org.alliancegenome.indexer.variant.scripts;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.zip.GZIPInputStream;

import org.alliancegenome.es.index.site.schema.settings.VariantIndexSettings;
import org.alliancegenome.es.util.*;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig.Builder;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback;
import org.elasticsearch.common.unit.*;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.core.TimeValue;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TestLoadJsonFile {

    public String indexName;

    private IndexManager im = new IndexManager(new VariantIndexSettings(true, 4));
    private ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);
    private LinkedBlockingDeque<String> jsonQueue = new LinkedBlockingDeque<String>(5000);
    
    public static void main(String[] args) throws Exception {
        new TestLoadJsonFile();
    }
    
    public TestLoadJsonFile() throws Exception {
        
        indexName = im.startSiteIndex();
        
        List<VCFJsonIndexer> indexers = new ArrayList<>();
        
        for(int i = 0; i < 4; i++) {
            VCFJsonIndexer indexer = new VCFJsonIndexer(new HttpHost("localhost", 9200 + i));
            indexer.start();
            indexers.add(indexer);
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File("/Users/olinblodgett/Desktop/Variants/HUMAN.v2.vep.chr16.vcf.json.aa.gz")))));
        ph.startProcess("Json Reader: ");
        
        String line = null;
        while((line = reader.readLine()) != null) {
            //log.info(line);
            jsonQueue.put(line);
            ph.progressProcess();
        }
        
        ph.finishProcess();
        
        log.info("Waiting for jsonQueue to empty");
        while(!jsonQueue.isEmpty()) {
            Thread.sleep(1000);
        }

        log.info("JSon Queue Empty shuting down indexers");
        for(VCFJsonIndexer i: indexers) {
            i.interrupt();
            i.join();
        }
        
        im.finishIndex();
    }
    
    private class VCFJsonIndexer extends Thread {
        private ProcessDisplayHelper ph3 = new ProcessDisplayHelper(2000);
        
        private BulkProcessor.Builder builder;
        private BulkProcessor bulkProcessor;
        private RestHighLevelClient client;
        
        public VCFJsonIndexer(HttpHost host) {
            client = new RestHighLevelClient(
                RestClient.builder(host).setRequestConfigCallback(
                    new RequestConfigCallback() {
                        @Override
                        public Builder customizeRequestConfig(Builder requestConfigBuilder) {
                            return requestConfigBuilder
                                    .setConnectTimeout(5000)
                                    .setSocketTimeout(1800000)
                                    .setConnectionRequestTimeout(1800000)
                                    ;
                        }
                    }
                )
            );
            BulkProcessor.Listener listener = new BulkProcessor.Listener() { 
                @Override
                public void beforeBulk(long executionId, BulkRequest request) {
                }

                @Override
                public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                    //log.info("Size: " + request.requests().size() + " MB: " + request.estimatedSizeInBytes() + " Time: " + response.getTook() + " Bulk Requet Finished");
                }

                @Override
                public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                    log.error("Bulk Request Failure: " + failure.getMessage());
                    for(DocWriteRequest<?> req: request.requests()) {
                        IndexRequest idxreq = (IndexRequest)req;
                        bulkProcessor.add(idxreq);
                    }
                    log.error("Finished Adding requests to Queue:");
                }
            };

            
            log.info("Creating Bulk Processor");
            builder = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener);
            builder.setBulkActions(1000);
            builder.setConcurrentRequests(2);
            builder.setBulkSize(new ByteSizeValue(100, ByteSizeUnit.MB));
            builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
            
            bulkProcessor = builder.build();
            
        }
        
        public void run() {
            ph3.startProcess("VCFJsonIndexer: ");
            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    String doc = jsonQueue.take();
                    bulkProcessor.add(new IndexRequest(indexName).source(doc, XContentType.JSON));
                    ph3.progressProcess();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ph3.finishProcess();
        }
    }
    
}
