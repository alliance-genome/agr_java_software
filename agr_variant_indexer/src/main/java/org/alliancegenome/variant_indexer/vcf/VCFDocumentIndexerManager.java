package org.alliancegenome.variant_indexer.vcf;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.variant_indexer.download.model.DownloadFileSet;
import org.alliancegenome.variant_indexer.download.model.DownloadSource;
import org.alliancegenome.variant_indexer.download.model.DownloadableFile;

public class VCFDocumentIndexerManager extends Thread {

    private DownloadFileSet downloadSet;

    public VCFDocumentIndexerManager(DownloadFileSet downloadSet) {
        this.downloadSet = downloadSet;
    }
    
    public void run() {
        
        try {

            ExecutorService executor = Executors.newFixedThreadPool(downloadSet.getDocumentIndexerThreads());
            
            for(DownloadSource source: downloadSet.getDownloadFileSet()) {
                for(DownloadableFile df: source.getFileList()) {
                    VCFDocumentIndexer indexer = new VCFDocumentIndexer(df);
                    executor.execute(indexer);
                }
            }
            
            executor.shutdown();  
            while (!executor.isTerminated()) {   } 
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
