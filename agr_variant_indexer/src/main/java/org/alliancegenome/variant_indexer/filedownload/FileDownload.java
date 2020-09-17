package org.alliancegenome.variant_indexer.filedownload;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.alliancegenome.variant_indexer.filedownload.model.DownloadableFile;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class FileDownload extends Thread {

    private String downloadPath;
    private DownloadableFile file;
    private URL downloadUrl;


    public FileDownload(DownloadableFile file, String downloadPath) {
        this.file = file;
        this.downloadPath = downloadPath;
    }

    private URL verifyUrl(String url) {
        // Only allow these URLs.
        if (!url.toLowerCase().startsWith("http://") &&
            !url.toLowerCase().startsWith("https://") &&
            !url.toLowerCase().startsWith("ftp://"))
            return null;

        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(url);
        } catch (Exception e) {
            return null;
        }

        if (verifiedUrl.getFile().length() < 2)
            return null;

        return verifiedUrl;
    }

    private String getFilePath(URL url) {
        String fileName = url.getFile();
        return fileName.substring(fileName.lastIndexOf('/') + 1);
    }

    public void run() {
        try {
            downloadUrl = verifyUrl(file.getUrl());
            if(downloadUrl == null) {
                System.out.println("Unable to verify file: " + file.getUrl());
                return;
            }
            log.info("Downloading: " + downloadUrl + " -> " + downloadPath);
            File dir = new File(downloadPath);
            if(!dir.exists()) {
                Files.createDirectories(Paths.get(downloadPath));
            }

            File localFile = new File(downloadPath + "/" + getFilePath(downloadUrl));
            file.setLocalGzipFilePath(localFile.getAbsolutePath());
            
            if(localFile.exists()) {
                log.warn("Local File: " + localFile.getAbsolutePath() +  " already exists: skipping");
                return;
            }
            
            InputStream in = downloadUrl.openStream();
            Files.copy(in, Paths.get(localFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
            log.info("Finished Downloading: " + downloadUrl + " -> " + localFile.getAbsolutePath());
        
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
