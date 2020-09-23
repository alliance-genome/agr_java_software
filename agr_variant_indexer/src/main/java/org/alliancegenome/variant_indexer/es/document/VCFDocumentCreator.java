package org.alliancegenome.variant_indexer.es.document;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

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
    
    private ProcessDisplayHelper ph1 = new ProcessDisplayHelper(12000);
    private ProcessDisplayHelper ph2 = new ProcessDisplayHelper(12000);
    private ProcessDisplayHelper ph3 = new ProcessDisplayHelper(12000);
    
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

        VCFJsonIndexer indexer = new VCFJsonIndexer();
        indexer.start();

        try {
            reader.join();
            ph1.finishProcess();
            for(VCFTransformer t: transformers) {
                t.join();
            }
            ph2.finishProcess();
            indexer.join();
            ph3.finishProcess();
            
            log.info("Threads finished: ");
            log.info("VC Queue: " + vcQueue.size());
            vcQueue.clear();
            log.info("Json Queue: " + jsonQueue.size());
            jsonQueue.clear();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    
    private class VCFReader extends Thread {
        public void run() {
            ph1.startProcess("VCFReader: " + vcfFilePath);
            VCFFileReader reader = new VCFFileReader(new File(vcfFilePath), false);
            CloseableIterator<VariantContext> iter1 = reader.iterator();
            try {
                while(iter1.hasNext()) {
                    VariantContext vc = iter1.next();
                    ph1.progressProcess();
                    vcQueue.offer(vc, 10, TimeUnit.DAYS);
                }
                vcQueue.add(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            reader.close();
        }
    }
    
    private class VCFTransformer extends Thread {
        public void run() {
            ph2.startProcess("VCFTransformer: " + speciesName);
            VariantContextConverter converter = VariantContextConverter.getConverter(speciesName);
            try {
                while(true) {
                    VariantContext ctx = vcQueue.take();
                    if(ctx == null) {
                        log.info("VCFTransformer: Recieved Null:");
                        vcQueue.add(null);
                        Thread.sleep(15000);
                        jsonQueue.add(null);
                        log.info("VCFTransformer: returning");
                        return;
                    }
                    List<String> docs = converter.convertVariantContext(ctx, taxon);
                    
                    for(String doc: docs) {
                        ph2.progressProcess("VCQueue: " + vcQueue.size() + " JsonQueue: " + jsonQueue.size());
                        jsonQueue.offer(doc, 10, TimeUnit.DAYS);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private class VCFJsonIndexer extends Thread {
        public void run() {
            ph3.startProcess("VCFJsonIndexer: " + indexName);
            try {
                while(true) {
                    String doc = jsonQueue.take();
                    if(doc == null) {
                        log.info("VCFJsonIndexer: Recieved Null: returning");
                        return;
                    }
                    ph3.progressProcess();
                    bulkProcessor.add(new IndexRequest(indexName).source(doc, XContentType.JSON));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
