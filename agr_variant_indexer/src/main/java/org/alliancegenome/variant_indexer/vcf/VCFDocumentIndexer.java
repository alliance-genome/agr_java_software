package org.alliancegenome.variant_indexer.vcf;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.alliancegenome.variant_indexer.download.model.DownloadableFile;
import org.alliancegenome.variant_indexer.es.ESDocumentInjector;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class VCFDocumentIndexer extends Thread {

    private DownloadableFile downloadFile;
    private ESDocumentInjector docInjector;

    public VCFDocumentIndexer(DownloadableFile df, ESDocumentInjector edi) {
        this.downloadFile = df;
        this.docInjector = edi;
    }

    public void run() {
        Date start = new Date();
        Date end = new Date();
        
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(downloadFile.getLocalJsonFilePath()))));
            String line;
            
            int record_count = 100000;
            int count = 0;
            
            while ((line = br.readLine()) != null) {
               docInjector.addDocument(line);
               
               
                if(count > 0 && count % record_count == 0) {
                    end = new Date();
                    log.info(downloadFile.getLocalJsonFilePath() + " Count: " + count + " r/s: " + ((record_count * 1000) / (end.getTime() - start.getTime())));
                    start = new Date();
                }
                count++;
               
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.info("Running Indexer");
    }
}
