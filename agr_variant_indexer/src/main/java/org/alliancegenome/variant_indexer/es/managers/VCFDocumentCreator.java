package org.alliancegenome.variant_indexer.es.managers;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.es.util.*;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.converters.VariantContextConverter;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadableFile;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.common.unit.*;
import org.elasticsearch.common.xcontent.XContentType;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VCFDocumentCreator extends Thread {

    private String localGzipFilePath;
    private SpeciesType speciesType;
    public static String indexName;
    private BulkProcessor.Builder builder1;
    private BulkProcessor.Builder builder2;
    private BulkProcessor.Builder builder3;
    private BulkProcessor.Builder builder4;
    
    private BulkProcessor bulkProcessor1;
    private BulkProcessor bulkProcessor2;
    private BulkProcessor bulkProcessor3;
    private BulkProcessor bulkProcessor4;
    
    private RestHighLevelClient client = EsClientFactory.getDefaultEsClient();
    
    private LinkedBlockingDeque<VariantContext> vcQueue = new LinkedBlockingDeque<VariantContext>(VariantConfigHelper.getDocumentCreatorContextQueueSize());
    private LinkedBlockingDeque<String> jsonQueue1 = new LinkedBlockingDeque<String>(10000); // Max 10K * 10K = 100M
    private LinkedBlockingDeque<String> jsonQueue2 = new LinkedBlockingDeque<String>(1333); // Max 75K * 1333 = 100M
    private LinkedBlockingDeque<String> jsonQueue3 = new LinkedBlockingDeque<String>(1000); // Max 100K * 1000 = 100M
    private LinkedBlockingDeque<String> jsonQueue4 = new LinkedBlockingDeque<String>(500); // Max 200K * 500 = 100M if documents are larger then we might need to split this down more

    public VCFDocumentCreator(DownloadableFile downloadFile, SpeciesType speciesType) {
        this.localGzipFilePath = downloadFile.getLocalGzipFilePath();
        this.speciesType = speciesType;
    }
    
    public void run() {
        
        log.info("Creating Bulk Processor 0 - 10K");
        builder1 = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() { 
            @Override
            public void beforeBulk(long executionId, BulkRequest request) { }
            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) { }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                log.error("Bulk Request Failure: " + failure.getMessage() + " " + localGzipFilePath);
                for(DocWriteRequest<?> req: request.requests()) {
                    IndexRequest idxreq = (IndexRequest)req;
                    bulkProcessor1.add(idxreq);
                }
                log.error("Finished Adding requests to Queue:");
            }
        });
        
        log.info("Creating Bulk Processor 10K - 75K");
        builder2 = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() { 
            @Override
            public void beforeBulk(long executionId, BulkRequest request) { }
            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) { }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                log.error("Bulk Request Failure: " + failure.getMessage() + " " + localGzipFilePath);
                for(DocWriteRequest<?> req: request.requests()) {
                    IndexRequest idxreq = (IndexRequest)req;
                    bulkProcessor2.add(idxreq);
                }
                log.error("Finished Adding requests to Queue:");
            }
        });
        
        log.info("Creating Bulk Processor 75K - 100K");
        builder3 = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() { 
            @Override
            public void beforeBulk(long executionId, BulkRequest request) { }
            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) { }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                log.error("Bulk Request Failure: " + failure.getMessage() + " " + localGzipFilePath);
                for(DocWriteRequest<?> req: request.requests()) {
                    IndexRequest idxreq = (IndexRequest)req;
                    bulkProcessor3.add(idxreq);
                }
                log.error("Finished Adding requests to Queue:");
            }
        });
        
        log.info("Creating Bulk Processor 100K - 200K");
        builder4 = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), new BulkProcessor.Listener() { 
            @Override
            public void beforeBulk(long executionId, BulkRequest request) { }
            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) { }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                log.error("Bulk Request Failure: " + failure.getMessage() + " " + localGzipFilePath);
                for(DocWriteRequest<?> req: request.requests()) {
                    IndexRequest idxreq = (IndexRequest)req;
                    bulkProcessor4.add(idxreq);
                }
                log.error("Finished Adding requests to Queue:");
            }
        });

        builder1.setBulkActions(1000);
        builder1.setConcurrentRequests(10);
        builder1.setBulkSize(new ByteSizeValue(10, ByteSizeUnit.MB));
        builder1.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
        bulkProcessor1 = builder1.build();
        
        builder2.setBulkActions(133);
        builder2.setConcurrentRequests(10);
        builder2.setBulkSize(new ByteSizeValue(10, ByteSizeUnit.MB));
        builder2.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
        bulkProcessor2 = builder2.build();
        
        builder3.setBulkActions(100);
        builder3.setConcurrentRequests(10);
        builder3.setBulkSize(new ByteSizeValue(10, ByteSizeUnit.MB));
        builder3.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
        bulkProcessor3 = builder3.build();
        
        builder4.setBulkActions(50);
        builder4.setConcurrentRequests(10);
        builder4.setBulkSize(new ByteSizeValue(10, ByteSizeUnit.MB));
        builder4.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));
        bulkProcessor4 = builder4.build();
        
        
        VCFReader reader = new VCFReader();
        reader.start();
        
        List<VCFTransformer> transformers = new ArrayList<>();
        
        for(int i = 0; i < VariantConfigHelper.getDocumentCreatorContextTransformerThreads(); i++) {
            VCFTransformer transformer = new VCFTransformer();
            transformer.start();
            transformers.add(transformer);
        }
        
        ArrayList<VCFJsonBulkIndexer> indexers = new ArrayList<>();
        
        for(int i = 0; i < VariantConfigHelper.getJsonIndexexBulkThreads(); i++) {
            VCFJsonBulkIndexer indexer1 = new VCFJsonBulkIndexer(jsonQueue1, bulkProcessor1); indexer1.start(); indexers.add(indexer1);
            VCFJsonBulkIndexer indexer2 = new VCFJsonBulkIndexer(jsonQueue2, bulkProcessor2); indexer2.start(); indexers.add(indexer2);
            VCFJsonBulkIndexer indexer3 = new VCFJsonBulkIndexer(jsonQueue3, bulkProcessor3); indexer3.start(); indexers.add(indexer3);
            VCFJsonBulkIndexer indexer4 = new VCFJsonBulkIndexer(jsonQueue4, bulkProcessor4); indexer4.start(); indexers.add(indexer4);
        }

        try {
            reader.join();
            log.info("Waiting for VC Queue to empty");
            while(!vcQueue.isEmpty()) {
                Thread.sleep(1000);
            }
            log.info("VC Queue Empty shuting down transformers");
            for(VCFTransformer t: transformers) {
                t.interrupt();
                t.join();
            }
            log.info("Transformers shutdown");
            
            log.info("Waiting for jsonQueue to empty");
            while(!(jsonQueue1.isEmpty() && jsonQueue2.isEmpty() && jsonQueue3.isEmpty() && jsonQueue4.isEmpty())) {
                Thread.sleep(1000);
            }
            
            log.info("JSon Queue Empty shuting down bulk indexers");
            for(VCFJsonBulkIndexer indexer: indexers) {
                indexer.interrupt();
                indexer.join();
            }
            log.info("Bulk Indexers shutdown");
            
            log.info("Threads finished: " + localGzipFilePath);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    
    private class VCFReader extends Thread {
        
        private ProcessDisplayHelper ph1 = new ProcessDisplayHelper(log, VariantConfigHelper.getDocumentCreatorDisplayInterval());
        
        public void run() {
            ph1.startProcess("VCFReader: " + localGzipFilePath);
            VCFFileReader reader = new VCFFileReader(new File(localGzipFilePath), false);
            CloseableIterator<VariantContext> iter1 = reader.iterator();
            try {
                while(iter1.hasNext()) {
                    VariantContext vc = iter1.next();
                    vcQueue.put(vc);
                    ph1.progressProcess();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            reader.close();
            ph1.finishProcess();
        }
    }
    
    private class VCFTransformer extends Thread {
        
        VariantContextConverter converter = VariantContextConverter.getConverter(speciesType);
        private ProcessDisplayHelper ph2 = new ProcessDisplayHelper(log, VariantConfigHelper.getDocumentCreatorDisplayInterval());
        
        public void run() {
            ph2.startProcess("VCFTransformer: " + speciesType.getName());
            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    VariantContext ctx = vcQueue.take();
                    List<String> docs = converter.convertVariantContext(ctx, speciesType);
                    for(String doc: docs) {
                        try {
                            if(doc.length() < 10000) {
                                jsonQueue1.put(doc);
                            } else if(doc.length() < 75000) {
                                jsonQueue2.put(doc);
                            } else if(doc.length() < 100000) {
                                jsonQueue3.put(doc);
                            } else {
                                jsonQueue4.put(doc);
                            }
                            ph2.progressProcess("VCQueue: " + vcQueue.size());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ph2.finishProcess();
        }
    }
    
    private class VCFJsonBulkIndexer extends Thread {
        private ProcessDisplayHelper ph3 = new ProcessDisplayHelper(log, VariantConfigHelper.getDocumentCreatorDisplayInterval());
        private LinkedBlockingDeque<String> jsonQueue;
        private BulkProcessor bulkProcessor;
        
        public VCFJsonBulkIndexer(LinkedBlockingDeque<String> jsonQueue, BulkProcessor bulkProcessor) {
            this.jsonQueue = jsonQueue;
            this.bulkProcessor = bulkProcessor;
        }
        
        public void run() {
            ph3.startProcess("VCFJsonIndexer: " + indexName);
            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    String doc = jsonQueue.take();
                    bulkProcessor.add(new IndexRequest(indexName).source(doc, XContentType.JSON));
                    ph3.progressProcess("JSon Queue: " + jsonQueue.size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ph3.finishProcess();
        }
    }

}
