package org.alliancegenome.indexer.variant.es.managers;

import java.util.concurrent.*;

import org.alliancegenome.core.filedownload.model.*;
import org.alliancegenome.core.variant.config.VariantConfigHelper;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class SourceDocumentCreationManager extends Thread {

    private DownloadFileSet downloadSet;
    
    public SourceDocumentCreationManager(DownloadFileSet downloadSet) {
        this.downloadSet = downloadSet;
    }

    public void run() {

        try {

            ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getSourceDocumentCreatorThreads());
            for(DownloadSource source: downloadSet.getDownloadFileSet()) {
                SourceDocumentCreation creator = new SourceDocumentCreation(source);
                executor.execute(creator);
            }

            log.info("SourceDocumentCreationManager shuting down executor: ");
            executor.shutdown();  
            while (!executor.isTerminated()) {
                Thread.sleep(1000);
            }
            log.info("SourceDocumentCreationManager executor shut down: ");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
