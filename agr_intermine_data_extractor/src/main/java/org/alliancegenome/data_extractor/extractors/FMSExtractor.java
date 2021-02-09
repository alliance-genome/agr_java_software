package org.alliancegenome.data_extractor.extractors;

import java.io.*;
import java.net.URL;
import java.util.concurrent.*;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.data_extractor.extractors.fms.*;
import org.alliancegenome.data_extractor.extractors.fms.interfaces.SnapShotRESTInterface;
import org.apache.commons.io.FileUtils;

import lombok.extern.log4j.Log4j2;
import si.mazi.rescu.RestProxyFactory;

@Log4j2
public class FMSExtractor extends DataExtractor {

    SnapShotRESTInterface api2;

    private LinkedBlockingDeque<Runnable> runningQueue = new LinkedBlockingDeque<Runnable>(10);

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 10, 10, TimeUnit.MILLISECONDS, runningQueue);

    public FMSExtractor() {
        executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                try {
                    executor.getQueue().offer(r, 10, TimeUnit.DAYS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void extract(PrintWriter writer) {
        api2 = RestProxyFactory.createProxy(SnapShotRESTInterface.class, ConfigHelper.getFMSUrl());
        SnapShotResponce res = api2.getSnapShot(ConfigHelper.getAllianceRelease());

        for(DataFile df: res.getSnapShot().getDataFiles()) {
            FMSDownload fd = new FMSDownload(df);
            executor.execute(fd);
        }
        try {
            while (!runningQueue.isEmpty()) Thread.sleep(100);
            executor.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        
    }

    @Override
    protected String getFileName() {
        return null;
    }
    
    @Override
    protected String getDirName() {
        return "fms";
    }

    public class FMSDownload implements Runnable {

        private DataFile df;

        public FMSDownload(DataFile df) {
            this.df = df;
        }

        public void run() {
            
            try {
                URL url = new URL(df.getS3Url());
                log.info("Downloading: " + url);
                if(df.getS3Url().endsWith(".gz") && !df.getFileName().endsWith(".gz")) {
                    File out = new File(ConfigHelper.getDataExtractorDirectory() + "/" + getDirName() + "/" + df.getFileName() + ".gz");
                    if(!out.exists()) {
                        FileUtils.copyURLToFile(url, out);
                        File outFile = new File(ConfigHelper.getDataExtractorDirectory() + "/" + getDirName() + "/" + df.getFileName());
                        decompressGzip(out, outFile);
                        out.delete();
                    }       
                    
                }else {
                    
                    File out = new File(ConfigHelper.getDataExtractorDirectory() + "/" + getDirName() + "/" + df.getFileName());
                     log.debug("saving file to " + out.getAbsolutePath());
                    if(!out.exists()) {
                        FileUtils.copyURLToFile(url, out);
                    } else {
                        log.info("File Exists Skipping: " + out.getAbsolutePath());
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    
    
    }
}
