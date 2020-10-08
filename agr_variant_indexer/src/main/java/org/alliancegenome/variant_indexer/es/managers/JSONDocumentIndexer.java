package org.alliancegenome.variant_indexer.es.managers;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;

import org.alliancegenome.es.util.*;
import org.alliancegenome.variant_indexer.config.VariantConfigHelper;
import org.alliancegenome.variant_indexer.filedownload.model.DownloadableFile;
import org.apache.commons.io.FilenameUtils;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.common.unit.*;
import org.elasticsearch.common.xcontent.XContentType;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class JSONDocumentIndexer extends Thread {

    private RestHighLevelClient client = EsClientFactory.getDefaultEsClient();
    
    private LinkedBlockingDeque<String> jsonQueue = new LinkedBlockingDeque<String>(1000);
    
    private BulkProcessor.Builder builder;
    private BulkProcessor bulkProcessor;
    public static String indexName;
    private DownloadableFile downloadFile;
    
    public JSONDocumentIndexer(DownloadableFile downloadFile) {
        this.downloadFile = downloadFile;
    }
    
    public void run() {
        
        
        BulkProcessor.Listener listener = new BulkProcessor.Listener() { 
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
                //log.info("Size: " + request.requests().size() + " MB: " + request.estimatedSizeInBytes() + " Time: " + response.getTook() + " Bulk Requet Finished");
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                log.error("Bulk Request Failure: " + failure.getMessage());
                for(DocWriteRequest<?> req: request.requests()) {
                    IndexRequest idxreq = (IndexRequest)req;
                    bulkProcessor.add(idxreq);
                }
                log.error("Finished Adding requests to Queue:");
            }
        };
        
        log.info("Creating Bulk Processor");
        builder = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener);
        builder.setBulkActions(VariantConfigHelper.getJsonIndexerEsBulkActionSize());
        builder.setConcurrentRequests(VariantConfigHelper.getJsonIndexerEsBulkConcurrentRequests());
        builder.setBulkSize(new ByteSizeValue(VariantConfigHelper.getJsonIndexerEsBulkSizeMB(), ByteSizeUnit.MB));
        builder.setFlushInterval(TimeValue.timeValueSeconds(180L));
        builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));

        bulkProcessor = builder.build();
        

        VCFJsonIndexer indexer = new VCFJsonIndexer();
        indexer.start();
        
        try {
            ProcessDisplayHelper ph = new ProcessDisplayHelper(log, VariantConfigHelper.getDocumentCreatorDisplayInterval());
            
            String fileName = FilenameUtils.removeExtension(downloadFile.getLocalGzipFilePath());
            String filePrefix = FilenameUtils.getName(fileName);
            String filePath = FilenameUtils.getFullPath(fileName);
            log.debug("fileName: " + fileName);
            log.debug("filePrefix: " + filePrefix);
            log.debug("filePath: " + filePath);
            
            ArrayList<String> documentFiles = new ArrayList<String>();
            
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(filePath), filePrefix + ".*.json.gz")) {
                dirStream.forEach(path -> documentFiles.add(path.toString()));
            }
            log.info("Files: " + documentFiles);


            ph.startProcess("Json Reader: ");

            for(String filePartPath: documentFiles) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(filePartPath)))));
                String line = null;
                while((line = reader.readLine()) != null) {
                    //log.info(line);
                    jsonQueue.put(line);
                    ph.progressProcess();
                }
                reader.close();
            }
            
            ph.finishProcess();

            
            log.info("Waiting for jsonQueue to empty");
            while(!jsonQueue.isEmpty()) {
                Thread.sleep(1000);
            }
        
            log.info("JSon Queue Empty shuting down indexer");
            indexer.interrupt();
            indexer.join();
            log.info("Indexer shutdown");
            
            log.info("Waiting for Bulk Processor to finish");
            bulkProcessor.flush();
            boolean finished = bulkProcessor.awaitClose(10, TimeUnit.DAYS);
            log.info("Bulk Processor finished: " + finished);
            
        
            log.info("Threads finished: " + downloadFile.getLocalGzipFilePath());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    private class VCFJsonIndexer extends Thread {
        private ProcessDisplayHelper ph3 = new ProcessDisplayHelper(log, VariantConfigHelper.getDocumentCreatorDisplayInterval());
        
        public void run() {
            ph3.startProcess("VCFJsonIndexer: " + indexName);
            while(!(Thread.currentThread().isInterrupted())) {
                try {
                    String doc = jsonQueue.take();
                    bulkProcessor.add(new IndexRequest(indexName).source(doc, XContentType.JSON));
                    ph3.progressProcess();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ph3.finishProcess();
        }
    }

}
