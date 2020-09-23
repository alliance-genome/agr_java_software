package org.alliancegenome.variant_indexer.es.document;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.converters.VariantContextConverter;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadableFile;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VCFDocumentCreator extends Thread {

    private String vcfFilePath;
    private int taxon;
    private String speciesName;
    
    private BulkProcessor.Builder builder;
    private BulkProcessor bulkProcessor;
    public static String indexName;
    
    private RestHighLevelClient client = EsClientFactory.createNewClient();

    private LinkedBlockingDeque<VariantContext> vcQueue = new LinkedBlockingDeque<VariantContext>(VariantConfigHelper.getDocumentCreatorContextQueueSize());
    private LinkedBlockingDeque<String> jsonQueue = new LinkedBlockingDeque<String>(VariantConfigHelper.getDocumentCreatorJsonQueueSize());

    public VCFDocumentCreator(DownloadableFile downloadFile, String speciesName, int taxon) {
        this.vcfFilePath = downloadFile.getLocalGzipFilePath();
        this.speciesName = speciesName;
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
        builder.setBulkActions(VariantConfigHelper.getEsBulkActionSize());
        builder.setConcurrentRequests(VariantConfigHelper.getEsBulkConcurrentRequests());
        builder.setBulkSize(new ByteSizeValue(VariantConfigHelper.getEsBulkSizeMB(), ByteSizeUnit.MB));
        builder.setFlushInterval(TimeValue.timeValueSeconds(180L));
        builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));

        bulkProcessor = builder.build();
        
        VCFReader reader = new VCFReader();
        reader.start();
        
        List<VCFTransformer> transformers = new ArrayList<>();
        
        for(int i = 0; i < VariantConfigHelper.getDocumentCreatorContextTransformerThreads(); i++) {
            VCFTransformer transformer = new VCFTransformer();
            transformer.start();
            transformers.add(transformer);
        }
        
        List<VCFJsonIndexer> indexers = new ArrayList<>();
        
        for(int i = 0; i < VariantConfigHelper.getDocumentCreatorContextIndexerThreads(); i++) {
            VCFJsonIndexer indexer = new VCFJsonIndexer();
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

            log.info("JSon Queue Empty shuting down indexers");
            for(VCFJsonIndexer i: indexers) {
                i.interrupt();
                i.join();
            }
            log.info("Indexers shutdown");

            log.info("Waiting for Bulk Processor to finish");
            bulkProcessor.flush();
            boolean finished = bulkProcessor.awaitClose(10, TimeUnit.DAYS);
            log.info("Bulk Processor finished: " + finished);
            
            log.info("Threads finished: " + vcfFilePath);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    
    private class VCFReader extends Thread {
        
        private ProcessDisplayHelper ph1 = new ProcessDisplayHelper(VariantConfigHelper.getDocumentCreatorDisplayInterval());
        
        public void run() {
            ph1.startProcess("VCFReader: " + vcfFilePath);
            VCFFileReader reader = new VCFFileReader(new File(vcfFilePath), false);
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
        
        VariantContextConverter converter = VariantContextConverter.getConverter(speciesName);
        private ProcessDisplayHelper ph2 = new ProcessDisplayHelper(VariantConfigHelper.getDocumentCreatorDisplayInterval());
        
        public void run() {
            ph2.startProcess("VCFTransformer: " + speciesName);
            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    VariantContext ctx = vcQueue.take();
                    List<String> docs = converter.convertVariantContext(ctx, taxon);
                    for(String doc: docs) {
                        try {
                            jsonQueue.put(doc);
                            ph2.progressProcess("VCQueue: " + vcQueue.size() + " JsonQueue: " + jsonQueue.size());
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
    
    private class VCFJsonIndexer extends Thread {
        private ProcessDisplayHelper ph3 = new ProcessDisplayHelper(VariantConfigHelper.getDocumentCreatorDisplayInterval());
        
        public void run() {
            ph3.startProcess("VCFJsonIndexer: " + indexName);
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
