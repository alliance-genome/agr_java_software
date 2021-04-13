package org.alliancegenome.core.filedownload.process;
import java.util.concurrent.*;

import org.alliancegenome.core.filedownload.FileDownload;
import org.alliancegenome.core.filedownload.model.*;
import org.alliancegenome.core.variant.config.VariantConfigHelper;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class FileDownloadManager extends Thread {

    private DownloadFileSet downloadSet;

    public FileDownloadManager(DownloadFileSet downloadSet) {
        this.downloadSet = downloadSet;
    }

    public void run() {
        
        if(downloadSet == null || downloadSet.getDownloadFileSet() == null)
            return;

        log.info("Starting downloading variant Files");
        
        ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getFileDownloadThreads());

        for(DownloadSource source: downloadSet.getDownloadFileSet()) {
            for(DownloadableFile df: source.getFileList()) {
                FileDownload fd = new FileDownload(df, downloadSet.getDownloadPath());
                executor.execute(fd);
            }
        }
        
        executor.shutdown();  
        while (!executor.isTerminated()) {   }  

        log.info("Finished downloading Files");
    }


}
