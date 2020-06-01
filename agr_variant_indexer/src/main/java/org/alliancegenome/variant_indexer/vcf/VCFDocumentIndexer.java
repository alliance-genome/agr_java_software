package org.alliancegenome.variant_indexer.vcf;

import org.alliancegenome.variant_indexer.download.model.DownloadableFile;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class VCFDocumentIndexer extends Thread {

    public VCFDocumentIndexer(DownloadableFile df) {
        
    }

    public void run() {
        log.info("Running Indexer");
    }
}
