package org.alliancegenome.variant_indexer.vcf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.download.model.DownloadableFile;
import org.alliancegenome.variant_indexer.util.VariantContextConverter;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class VCFDocumentCreator extends Thread {

    private String vcfFilePath;
    private String outFilePath;
    
    private PrintWriter outFilePrinter;

    private ThreadPoolExecutor variantContextProcessorTaskExecuter = new ThreadPoolExecutor(
        1, 
        VariantConfigHelper.getContextProcessorTaskThreads(), 
        10, 
        TimeUnit.MILLISECONDS, 
        new LinkedBlockingDeque<Runnable>(VariantConfigHelper.getContextProcessorTaskQueueSize())
    );
    
    private ThreadPoolExecutor variantDocumentWriterTaskExecuter = new ThreadPoolExecutor(
        1,
        VariantConfigHelper.getDocumentWriterTaskThreads(),
        10,
        TimeUnit.MILLISECONDS,
        new LinkedBlockingDeque<Runnable>(VariantConfigHelper.getDocumentWriterTaskQueueSize())
    );
    
    public VCFDocumentCreator(DownloadableFile downloadFile) {
        this.vcfFilePath = downloadFile.getLocalGzipFilePath();
        downloadFile.setLocalJsonFilePath(vcfFilePath.replace("vcf.gz", "json.gz"));
        this.outFilePath = downloadFile.getLocalJsonFilePath();
    }

    public void run() {
        int count = 0;
        log.info("Processing File: " + vcfFilePath);
        
        File outputFile = new File(outFilePath);
        if(outputFile.exists()) {
            log.info("Json output file: " + outFilePath + " already exists");
            return;
        }
        
        Date start = new Date();
        Date end = new Date();
        try {
            outFilePrinter = new PrintWriter(new GZIPOutputStream(new FileOutputStream(outputFile)), true);

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
            
            variantDocumentWriterTaskExecuter.setRejectedExecutionHandler(new RejectedExecutionHandler() {
                public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                    try {
                        executor.getQueue().offer(r, 10, TimeUnit.DAYS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            

            int record_count = 1000000;
            
            List<VariantContext> workChunk = new ArrayList<>();

            while(iter1.hasNext()) {
                try {
                    VariantContext vc = iter1.next();
                    workChunk.add(vc);
                    
                    if(workChunk.size() >= VariantConfigHelper.getDocumentCreatorWorkChunkSize()) {
                        variantContextProcessorTaskExecuter.execute(new VariantContextProcessorTask(workChunk));
                        workChunk = new ArrayList<>();
                    }

                    if(count > 0 && count % record_count == 0) {
                        end = new Date();
                        log.info(outFilePath + " Count: " + count + " r/s: " + ((record_count * 1000) / (end.getTime() - start.getTime())));
                        start = new Date();
                    }
                    count++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(workChunk.size() > 0) {
                variantContextProcessorTaskExecuter.execute(new VariantContextProcessorTask(workChunk));
            }
            end = new Date();
            log.info(outFilePath + " Finished Count: " + count + " r/s: " + (((count % record_count) * 1000) / (end.getTime() - start.getTime())));
            log.info(outFilePath + " Finished Processing File: " + vcfFilePath);
            log.info(outFilePath + " Final: Count: " + count);

            variantContextProcessorTaskExecuter.shutdown();
            while (!variantContextProcessorTaskExecuter.isTerminated()) {
            }
            variantDocumentWriterTaskExecuter.shutdown();
            while (!variantDocumentWriterTaskExecuter.isTerminated()) {
            }
            
            log.debug("Finished all threads");
            reader.close();
            outFilePrinter.close();
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
                    variantDocumentWriterTaskExecuter.execute(new VariantDocumentWriterTask(doc));
                }
            }
        }
    }
    
    private class VariantDocumentWriterTask implements Runnable {
        private String document;
        
        public VariantDocumentWriterTask(String document) {
            this.document = document;
        }
        
        public void run() {
            outFilePrinter.println(document);
        }
    }

}
