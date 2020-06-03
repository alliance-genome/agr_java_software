package org.alliancegenome.variant_indexer.vcf;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.download.model.DownloadFileSet;
import org.alliancegenome.variant_indexer.download.model.DownloadSource;
import org.alliancegenome.variant_indexer.download.model.DownloadableFile;
import org.alliancegenome.variant_indexer.es.ESDocumentInjector;

public class VCFDocumentIndexerManager extends Thread {

    private DownloadFileSet downloadSet;
    private ESDocumentInjector edi = new ESDocumentInjector(true);

    public VCFDocumentIndexerManager(DownloadFileSet downloadSet) {
        this.downloadSet = downloadSet;
    }
    
    public void run() {
        
        try {

            ExecutorService executor = Executors.newFixedThreadPool(VariantConfigHelper.getDocumentIndexerThreads());
            
            for(DownloadSource source: downloadSet.getDownloadFileSet()) {
                for(DownloadableFile df: source.getFileList()) {
                    VCFDocumentIndexer indexer = new VCFDocumentIndexer(df, edi);
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
