package org.alliancegenome.variant_indexer.filedownload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.alliancegenome.variant_indexer.filedownload.model.DownloadableFile;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class FileDownloadFilter extends Thread {

    private DownloadableFile downloadFile;

    public FileDownloadFilter(DownloadableFile df) {
        this.downloadFile = df;
    }

    public void run() {
        String filePath = downloadFile.getLocalGzipFilePath();
        try {
            
            
            if(filePath.endsWith("homo_sapiens_incl_consequences-chr3.vcf.gz")) {
                
                log.info("Filtering: " + filePath);
                String newFilePath = filePath.replace(".vcf.gz", ".filtered.vcf.gz");
                File newFile = new File(newFilePath);
                
                downloadFile.setLocalGzipFilePath(newFilePath);
                
                if(newFile.exists()) {
                    log.info("File already filted: " + newFilePath);
                    return;
                }
                
                PrintWriter outFile = new PrintWriter(new GZIPOutputStream(new FileOutputStream(newFilePath)), true);
                BufferedReader inFile = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filePath))));
                String line;
                
                Date start = new Date();
                Date end = new Date();
                int record_count = 100000;
                int count = 0;
                
                while ((line = inFile.readLine()) != null) {
                    if(count == 4226490 || count == 4226491) {
                        log.info("Filtering out Line: ");
                        log.info(line);
                    } else {
                        outFile.println(line);
                    }
                    
                    if(count > 0 && count % record_count == 0) {
                        end = new Date();
                        log.info("Count: " + count + " r/s: " + ((record_count * 1000) / (end.getTime() - start.getTime())));
                        start = new Date();
                    }
                    count++;

                }
                inFile.close();
                outFile.close();

                log.info(downloadFile.getLocalGzipFilePath() + ": Need to filter out bad line in file");
            }
        } catch (Exception e) {
            
        }
    }
}
