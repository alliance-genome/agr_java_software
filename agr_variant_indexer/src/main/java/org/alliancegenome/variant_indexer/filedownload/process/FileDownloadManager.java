package org.alliancegenome.variant_indexer.filedownload.process;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.filedownload.FileDownload;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadFileSet;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadSource;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadableFile;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class FileDownloadManager extends Thread {

    private DownloadFileSet downloadSet;

    public FileDownloadManager(DownloadFileSet downloadSet) {
        this.downloadSet = downloadSet;
    }

    public void run() {
        
        ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getFileDownloadThreads());
        
        for(DownloadSource source: downloadSet.getDownloadFileSet()) {
            for(DownloadableFile df: source.getFileList()) {
                FileDownload fd = new FileDownload(df, downloadSet.getDownloadPath());
                executor.execute(fd);
            }
        }
        
        executor.shutdown();  
        while (!executor.isTerminated()) {   }  

        log.info("Downloading Files have finished downloading");
    }


}
