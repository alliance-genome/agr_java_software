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
    private BulkProcessor.Builder builder;
    private BulkProcessor bulkProcessor;

    private RestHighLevelClient client = EsClientFactory.getDefaultEsClient();
    
    private LinkedBlockingDeque<VariantContext> vcQueue = new LinkedBlockingDeque<VariantContext>(VariantConfigHelper.getDocumentCreatorContextQueueSize());
    LinkedBlockingDeque<String> jsonQueue = new LinkedBlockingDeque<String>(VariantConfigHelper.getDocumentCreatorJsonQueueSize());

    public VCFDocumentCreator(DownloadableFile downloadFile, SpeciesType speciesType) {
        this.localGzipFilePath = downloadFile.getLocalGzipFilePath();
        this.speciesType = speciesType;
    }
    
    public void run() {

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
                log.error("Bulk Request Failure: " + failure.getMessage() + " " + localGzipFilePath);
                for(DocWriteRequest<?> req: request.requests()) {
                    IndexRequest idxreq = (IndexRequest)req;
                    bulkProcessor.add(idxreq);
                }
                log.error("Finished Adding requests to Queue:");
            }
        };
        
        log.info("Creating Bulk Processor");
        builder = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener);
        //builder.setBulkActions(-1);
        builder.setBulkActions(100000);
        builder.setConcurrentRequests(VariantConfigHelper.getJsonIndexerEsBulkConcurrentRequests());
        builder.setBulkSize(new ByteSizeValue(VariantConfigHelper.getJsonIndexerEsBulkSizeMB(), ByteSizeUnit.MB));
        //builder.setFlushInterval(TimeValue.timeValueSeconds(180L));
        builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));

        bulkProcessor = builder.build();
        
        
        
        
        
        VCFReader reader = new VCFReader();
        reader.start();
        
        List<VCFTransformer> transformers = new ArrayList<>();
        
        for(int i = 0; i < VariantConfigHelper.getDocumentCreatorContextTransformerThreads(); i++) {
            VCFTransformer transformer = new VCFTransformer(jsonQueue);
            transformer.start();
            transformers.add(transformer);
        }
        
        ArrayList<VCFJsonBulkIndexer> indexers = new ArrayList<>();
        
        for(int i = 0; i < VariantConfigHelper.getJsonIndexexBulkThreads(); i++) {
            VCFJsonBulkIndexer indexer = new VCFJsonBulkIndexer();
            indexer.start();
            indexers.add(indexer);
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
            while(!jsonQueue.isEmpty()) {
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
        private LinkedBlockingDeque<String> jsonQueue;
        
        public VCFTransformer(LinkedBlockingDeque<String> jsonQueue) {
            this.jsonQueue = jsonQueue;
        }
        
        public void run() {
            ph2.startProcess("VCFTransformer: " + speciesType.getName());
            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    VariantContext ctx = vcQueue.take();
                    List<String> docs = converter.convertVariantContext(ctx, speciesType);
                    for(String doc: docs) {
                        try {
                            jsonQueue.put(doc);
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
