package org.alliancegenome.variant_indexer.es.managers;

import java.util.concurrent.*;

import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadFileSet;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadSource;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadableFile;
import org.elasticsearch.action.bulk.BulkRequest;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class VCFDocumentCreationManager extends Thread {

    private DownloadFileSet downloadSet;

    public VCFDocumentCreationManager(DownloadFileSet downloadSet) {
        this.downloadSet = downloadSet;
    }

    public void run() {

        try {
            
            ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getDocumentCreatorThreads());

            for(DownloadSource source: downloadSet.getDownloadFileSet()) {
                for(DownloadableFile df: source.getFileList()) {
                    VCFDocumentCreator creator = new VCFDocumentCreator(df, SpeciesType.getTypeByID(source.getTaxonId()));
                    executor.execute(creator);
                }
            }
            
            log.info("VCFDocumentCreationManager shuting down executor: ");
            executor.shutdown();  
            while (!executor.isTerminated()) {
                Thread.sleep(1000);
            }
            log.info("VCFDocumentCreationManager executor shut down: ");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
