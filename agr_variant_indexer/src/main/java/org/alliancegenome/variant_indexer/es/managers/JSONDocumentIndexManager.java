package org.alliancegenome.variant_indexer.es.managers;

import java.util.concurrent.*;

import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.filedownload.model.*;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class JSONDocumentIndexManager extends Thread {
    
    private DownloadFileSet downloadSet;
    
    public JSONDocumentIndexManager(DownloadFileSet downloadSet) {
        this.downloadSet = downloadSet;
    }

    public void run() {

        try {

            ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getJsonIndexerThreads());

            for(DownloadSource source: downloadSet.getDownloadFileSet()) {
                for(DownloadableFile df: source.getFileList()) {
                    JSONDocumentIndexer indexer = new JSONDocumentIndexer(df);
                    executor.execute(indexer);
                }
            }
            log.info("JSONDocumentIndexManager shuting down executor: ");
            executor.shutdown();  
            while (!executor.isTerminated()) {
                Thread.sleep(1000);
            }
            log.info("JSONDocumentIndexManager executor shut down: ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
