package org.alliancegenome.variant_indexer.vcf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.download.model.DownloadableFile;
import org.alliancegenome.variant_indexer.es.ESDocumentInjector;
import org.alliancegenome.variant_indexer.util.VariantContextConverter;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VCFDocumentCreator extends Thread {

    private String vcfFilePath;

    private ESDocumentInjector docInjector;
    private ProcessDisplayHelper ph = new ProcessDisplayHelper();

    private ThreadPoolExecutor variantContextProcessorTaskExecuter = new ThreadPoolExecutor(
        1, 
        VariantConfigHelper.getContextProcessorTaskThreads(), 
        10, 
        TimeUnit.MILLISECONDS, 
        new LinkedBlockingDeque<Runnable>(VariantConfigHelper.getContextProcessorTaskQueueSize())
    );
    
    public VCFDocumentCreator(DownloadableFile downloadFile) {
        this.vcfFilePath = downloadFile.getLocalGzipFilePath();
    }

    public void run() {

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
                        variantContextProcessorTaskExecuter.execute(new VariantContextProcessorTask(workChunk));
                        workChunk = new ArrayList<>();
                    }
                    ph.progressProcess();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(workChunk.size() > 0) {
                variantContextProcessorTaskExecuter.execute(new VariantContextProcessorTask(workChunk));
            }
            ph.finishProcess();

            variantContextProcessorTaskExecuter.shutdown();
            while (!variantContextProcessorTaskExecuter.isTerminated()) {
                Thread.sleep(1000);
            }
            
            log.debug("Finished all threads");
            reader.close();
            docInjector.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class VariantContextProcessorTask implements Runnable {

        private List<VariantContext> workChunk;

        public VariantContextProcessorTask(List<VariantContext> workChunk) {
            this.workChunk = workChunk;
        }

        public void run() {
            for(VariantContext ctx: workChunk) {
                List<String> docs = VariantContextConverter.convertVariantContext(ctx);
                
                for(String doc: docs) {
                    docInjector.addDocument(doc);
                }
            }
        }
    }

}
