package org.alliancegenome.variant_indexer.download;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.download.model.DownloadFileSet;
import org.alliancegenome.variant_indexer.download.model.DownloadSource;
import org.alliancegenome.variant_indexer.download.model.DownloadableFile;

public class FileDownloadFilterManager extends Thread {

    private DownloadFileSet downloadSet;

    public FileDownloadFilterManager(DownloadFileSet downloadSet) {
        this.downloadSet = downloadSet;
    }
    
    public void run() {
        
        try {

            ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getFileDownloadFilterThreads());
            
            for(DownloadSource source: downloadSet.getDownloadFileSet()) {
                for(DownloadableFile df: source.getFileList()) {
                    FileDownloadFilter filter = new FileDownloadFilter(df);
                    executor.execute(filter);
                }
            }
            
            executor.shutdown();  
            while (!executor.isTerminated()) {
                Thread.sleep(100);
            } 
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
