package org.alliancegenome.variant_indexer.es.document;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.converters.VariantContextConverter;
import org.alliancegenome.variant_indexer.es.ESDocumentInjector;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadableFile;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VCFDocumentCreator extends Thread {

    private String vcfFilePath;
    private int taxon;
    private ESDocumentInjector docInjector;
    private ProcessDisplayHelper ph = new ProcessDisplayHelper(10000);

    private double json_avg;

    private LinkedBlockingDeque<Runnable> runningQueue = new LinkedBlockingDeque<Runnable>(VariantConfigHelper.getContextProcessorTaskQueueSize());
    
    private VariantContextConverter converter;
    
    private ThreadPoolExecutor variantContextProcessorTaskExecuter = new ThreadPoolExecutor(
        1, 
        VariantConfigHelper.getContextProcessorTaskThreads(), 
        10, 
        TimeUnit.MILLISECONDS, 
        runningQueue
    );
    
    public VCFDocumentCreator(DownloadableFile downloadFile, String speciesName, int taxon) {
        this.vcfFilePath = downloadFile.getLocalGzipFilePath();
        this.taxon=taxon;
        converter = VariantContextConverter.getConverter(speciesName);
    }

    public void run() {

        File indexFile = new File(vcfFilePath + ".indexed");
        if(indexFile.exists()) {
            log.info("File Already Processed: " + vcfFilePath);
            return;
        }
        
        docInjector = new ESDocumentInjector();
        
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
            
            indexFile.createNewFile();
            
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
                    //docInjector.addDocument(doc);
                }
            }
        }
    }

}
