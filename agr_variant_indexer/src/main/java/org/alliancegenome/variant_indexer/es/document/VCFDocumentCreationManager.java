package org.alliancegenome.variant_indexer.es.document;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadFileSet;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadSource;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadableFile;

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
                    VCFDocumentCreator creator = new VCFDocumentCreator(df, source.getSpecies(),source.getTaxon());
                    executor.execute(creator);
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
