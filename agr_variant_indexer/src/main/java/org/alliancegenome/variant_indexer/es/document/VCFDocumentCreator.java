package org.alliancegenome.variant_indexer.es.document;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.converters.VariantContextConverter;
import org.alliancegenome.variant_indexer.es.ESDocumentInjector;
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
    
    private BulkProcessor.Builder builder;
    private BulkProcessor bulkProcessor;
    public static String indexName;
    private ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);

    private double json_avg;

    private LinkedBlockingDeque<Runnable> runningQueue = new LinkedBlockingDeque<Runnable>(VariantConfigHelper.getContextProcessorTaskQueueSize());
    
    private VariantContextConverter converter;
    private RestHighLevelClient client = EsClientFactory.createNewClient();
    
    private ThreadPoolExecutor variantContextProcessorTaskExecuter = new ThreadPoolExecutor(
        1, 
        VariantConfigHelper.getContextProcessorTaskThreads(), 
        10, 
        TimeUnit.MILLISECONDS, 
        runningQueue
    );
    
    public VCFDocumentCreator(DownloadableFile downloadFile, String speciesName, int taxon) {
        this.vcfFilePath = downloadFile.getLocalGzipFilePath();
        this.taxon = taxon;
        converter = VariantContextConverter.getConverter(speciesName);

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
        builder.setConcurrentRequests(VariantConfigHelper.getEsBulkConcurrentRequestsAmount());
        builder.setBulkSize(new ByteSizeValue(VariantConfigHelper.getEsBulkSizeMB(), ByteSizeUnit.MB));
        builder.setFlushInterval(TimeValue.timeValueSeconds(180L));
        builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));

        bulkProcessor = builder.build();

    }

    public void run() {

        ph.startProcess("Starting File: " + vcfFilePath, 0);

        try {
            VCFFileReader reader = new VCFFileReader(new File(vcfFilePath), false);
            CloseableIterator<VariantContext> iter1 = reader.iterator();

            variantContextProcessorTaskExecuter.setRejectedExecutionHandler(new RejectedExecutionHandler() {
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    try {
                        executor.getQueue().offer(r, 10, TimeUnit.DAYS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            List<VariantContext> workChunk = new ArrayList<>();

            while(iter1.hasNext()) {
                try {
                    VariantContext vc = iter1.next();
                    workChunk.add(vc);
                    
                    if(workChunk.size() >= VariantConfigHelper.getDocumentCreatorWorkChunkSize()) {
                        variantContextProcessorTaskExecuter.execute(new VariantContextProcessorTask(workChunk, taxon));
                        workChunk = new ArrayList<>();
                    }
                    ph.progressProcess("Avg Doc Size: " + json_avg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(workChunk.size() > 0) {
                variantContextProcessorTaskExecuter.execute(new VariantContextProcessorTask(workChunk, taxon));
            }
            ph.finishProcess();

            while(runningQueue.size() > 0) {
                Thread.sleep(1000);
            }
            
            variantContextProcessorTaskExecuter.shutdown();
            while (!variantContextProcessorTaskExecuter.isTerminated()) {
                Thread.sleep(1000);
            }
            
            log.debug("Finished all threads");
            
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    private double runningAverage(double avg, double new_sample, int size) {
        return (avg * (size - 1) / size) + (new_sample / size);
    }

    private class VariantContextProcessorTask implements Runnable {

        private List<VariantContext> workChunk;
        private int taxon;
        public VariantContextProcessorTask(List<VariantContext> workChunk, int taxon) {
            this.workChunk = workChunk;
            this.taxon = taxon;
        }

        public void run() {
            for(VariantContext ctx: workChunk) {
                List<String> docs = converter.convertVariantContext(ctx, taxon);
                
                for(String doc: docs) {
                    json_avg = runningAverage(json_avg, doc.length(), 1_000_000);
                    bulkProcessor.add(new IndexRequest(indexName).source(doc, XContentType.JSON));
                }
            }
        }
    }

}
