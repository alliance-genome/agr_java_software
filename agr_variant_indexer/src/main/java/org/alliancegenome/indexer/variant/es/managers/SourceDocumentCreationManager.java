package org.alliancegenome.indexer.variant.es.managers;

import java.util.concurrent.*;

import org.alliancegenome.core.filedownload.model.*;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.elasticsearch.client.RestHighLevelClient;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class SourceDocumentCreationManager extends Thread {

    private DownloadFileSet downloadSet;
    private RestHighLevelClient client;

    public SourceDocumentCreationManager(RestHighLevelClient client, DownloadFileSet downloadSet) {
        this.client = client;
        this.downloadSet = downloadSet;
    }

    public void run() {

        try {

            ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getSourceDocumentCreatorThreads());
            for(DownloadSource source: downloadSet.getDownloadFileSet()) {
                SourceDocumentCreation creator = new SourceDocumentCreation(client, source);
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
