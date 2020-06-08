package org.alliancegenome.variant_indexer.download;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.alliancegenome.variant_indexer.download.model.DownloadableFile;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
class FileDownload extends Thread {

    private static final int MAX_BUFFER_SIZE = 1024;
    public static final String STATUSES[] = {"Downloading", "Paused", "Complete", "Cancelled", "Error"};

    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;

    private String downloadPath;
    private DownloadableFile file;
    private URL downloadUrl;
    private int lastPercent = 0;

    @Getter
    private long size;
    @Getter
    private float avgSpeed = 0;
    @Getter
    private float speed = 0;
    @Getter
    private int status;

    private long downloaded;
    private long initTime;
    private long startTime;
    private long readSinceStart;
    private long elapsedTime = 0;
    private long prevElapsedTime = 0;
    private long remainingTime = -1;

    // Constructor for Download.
    public FileDownload(DownloadableFile file, String downloadPath) {
        this.file = file;
        this.downloadPath = downloadPath;
        size = -1;
        downloaded = 0;
        status = DOWNLOADING;
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

    public String getUrl() {
        return file.getUrl();
    }
    public String getElapsedTime() {
        return formatTime(elapsedTime / 1000000000);
    }
    public String getRemainingTime() {
        if(remainingTime < 0) return "Unknown";
        else return formatTime(remainingTime);
    }
    
    public String formatTime(long time) {
        String s = "";
        s += (String.format("%02d", time / 3600)) + ":";
        time %= 3600;
        s += (String.format("%02d", time / 60)) + ":";
        time %= 60;
        s += String.format("%02d", time);
        return s;
    }

    public float getProgress() {
        return ((float) downloaded / size) * 100;
    }

    private void error() {
        prevElapsedTime = elapsedTime;
        status = ERROR;
        stateChanged();
    }

    private String getFilePath(URL url) {
        String fileName = url.getFile();
        return fileName.substring(fileName.lastIndexOf('/') + 1);
    }

    public void run() {
        RandomAccessFile outputFile = null;
        InputStream stream = null;

        try {
            
            downloadUrl = verifyUrl(file.getUrl());
            if(downloadUrl == null) {
                System.out.println("Unable to verify file: " + file.getUrl());
                return;
            }

            HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();

            connection.setRequestProperty("Range", "bytes=" + downloaded + "-");

            connection.connect();

            if (connection.getResponseCode() / 100 != 2) {
                error();
            }

            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                error();
            }

            if (size == -1) {
                size = contentLength;
            }
            // used to update speed at regular intervals
            int i = 0;
            
            // If dir does not exist create it
            File dir = new File(downloadPath);
            if(!dir.exists()) {
                Files.createDirectories(Paths.get(downloadPath));
            }
            
            File localFile = new File(downloadPath + "/" + getFilePath(downloadUrl));
            file.setLocalGzipFilePath(localFile.getAbsolutePath());
            
            if(localFile.exists() && localFile.length() == size) {
                log.info("File Already download: " + localFile.getAbsolutePath());
                connection.disconnect();
                return;
            } else {
                log.info("Downloading: " + downloadUrl + " to: " + localFile.getAbsolutePath());
            }
            
            outputFile = new RandomAccessFile(localFile, "rw");
            outputFile.seek(downloaded);

            stream = connection.getInputStream();
            initTime = System.nanoTime();
            while (status == DOWNLOADING) {
                /* Size buffer according to how much of the file is left to download. */
                if(i == 0) {
                    startTime = System.nanoTime();
                    readSinceStart = 0;
                }
                byte buffer[];
                if (size - downloaded > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[(int)(size - downloaded)];
                }
                // Read from server into buffer.
                int read = stream.read(buffer);
                if (read == -1)
                    break;
                // Write buffer to file.
                outputFile.write(buffer, 0, read);
                downloaded += read;
                readSinceStart += read;
                //update speed
                i++;
                if(i >= 50) {
                    speed = (readSinceStart * 976562.5f) / (System.nanoTime() - startTime);
                    if(speed > 0) remainingTime = (long)((size - downloaded) / (speed * 1024));
                    else remainingTime = -1;
                    elapsedTime = prevElapsedTime + (System.nanoTime() - initTime);
                    avgSpeed = (downloaded * 976562.5f) / elapsedTime;
                    i = 0;
                }
                stateChanged();
            }

            /* Change status to complete if this point was reached because downloading has finished. */
            if (status == DOWNLOADING) {
                status = COMPLETE;
                stateChanged(true);
            }
            log.info("Finished Downloading: " + downloadUrl + " to: " + localFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println(e);
            error();
        } finally {
            if (file != null) {
                try {
                    outputFile.close();
                } catch (Exception e) {}
            }

            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {}
            }
        }
    }
    
    private void stateChanged() {
        stateChanged(false);
    }
    
    private void stateChanged(boolean show) {
        if(lastPercent != (int)getProgress() || show) {
            log.info("File: " + getFilePath(downloadUrl) + ": " + (int)getProgress() + "% " + getElapsedTime() + " AvgSpeed: " + avgSpeed + " Rate: " + speed);
            lastPercent = (int)getProgress();
        }
    }

}