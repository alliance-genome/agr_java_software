package org.alliancegenome.indexer.indexers;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
public abstract class Indexer<D extends ESDocument> extends Thread {

    public static String indexName;
    private Logger log = LogManager.getLogger(getClass());
    protected IndexerConfig indexerConfig;
    private RestHighLevelClient client;
    protected Runtime runtime = Runtime.getRuntime();
    protected DecimalFormat df = new DecimalFormat("#");
    protected ObjectMapper om = new ObjectMapper();

    protected String species = null;

    // Used for showing progress
    private Date startTime = new Date();
    private Date lastTime = new Date();
    private int lastSize;
    private long batchTotalSize = 0;
    private long batchCount = 0;

    protected Map<String,Double> popularityScore;

    protected BulkProcessor bulkProcessor;

    public Indexer(IndexerConfig indexerConfig) {
        this.indexerConfig = indexerConfig;

        om.setSerializationInclusion(Include.NON_NULL);

        loadPopularityScore();

        client = EsClientFactory.getDefaultEsClient();

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
                log.error("Finished Adding failed requests to bulkProcessor: ");
            }
        };

        BulkProcessor.Builder builder = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener);
        builder.setBulkActions(ConfigHelper.getEsBulkActionSize());
        builder.setBulkSize(new ByteSizeValue(ConfigHelper.getEsBulkSizeMB(), ByteSizeUnit.MB));
        builder.setConcurrentRequests(ConfigHelper.getEsBulkConcurrentRequests());
        builder.setFlushInterval(TimeValue.timeValueSeconds(180L));
        builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));

        bulkProcessor = BulkProcessor.builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener).build();

    }

    private void loadPopularityScore() {

        popularityScore = new HashMap<>();

        Path popularityFile = Paths.get(ConfigHelper.getPopularityFileName());
        if (!Files.exists(popularityFile)) {
            try {
                FileUtils.copyURLToFile(new URL(ConfigHelper.getPopularityDownloadUrl()), popularityFile.toFile());
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        try {
            popularityScore = Files.lines(popularityFile)
                    .collect(Collectors.toMap(key -> String.valueOf(key.split("\t")[0]), val -> Double.valueOf(val.split("\t")[1])));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    protected abstract void index();

    public void runIndex() {
        try {
            index();
            log.info("Waiting for bulkProcessor to finish");
            bulkProcessor.flush();
            bulkProcessor.awaitClose(30L, TimeUnit.DAYS);
            log.info("bulkProcessor finished");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public void run() {
        super.run();
        try {
            index();
            log.info("Waiting for bulkProcessor to finish");
            bulkProcessor.flush();
            bulkProcessor.awaitClose(30L, TimeUnit.DAYS);
            log.info("bulkProcessor finished");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }


    public void indexDocuments(Iterable<D> docs) {
        checkMemory();

        for (D doc : docs) {
            String json = null;
            try {
                json = om.writeValueAsString(doc);
                bulkProcessor.add(new IndexRequest(indexName).id(doc.getDocumentId()).source(json, XContentType.JSON));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    // Used to show progress when using a queue
    private void startProcess(int totalDocAmount) {
        log.info("Start Indexing: queue size: " + getBigNumber(totalDocAmount));
        lastTime = new Date();
        lastSize = totalDocAmount;
    }

    // Used to show process when using a queue
    private void progress(int currentSize, int totalDocAmount) {
        double percent = ((double) (totalDocAmount - currentSize) / (double) totalDocAmount);
        Date now = new Date();
        long diff = now.getTime() - startTime.getTime();
        long time = (now.getTime() - lastTime.getTime());
        int processedAmount = (lastSize - currentSize);
        String message = "" + getBigNumber(totalDocAmount - currentSize) + " records [" + getBigNumber(totalDocAmount) + "] ";
        message += (int) (percent * 100) + "% took: " + (time / 1000) + "s to process " + processedAmount;

        int batchAvg = 0;
        if (batchCount > 0) {
            batchAvg = (int) (batchTotalSize / batchCount);
        }
        message += " rate: " + ((processedAmount * 1000) / time) + "r/s ABS: " + batchAvg;

        if (percent > 0) {
            int perms = (int) (diff / percent);
            Date end = new Date(startTime.getTime() + perms);
            String expectedDuration = getHumanReadableTimeDisplay(end.getTime() - (new Date()).getTime());
            message += ", Memory: " + df.format(memoryPercent() * 100) + "%, ETA: " + expectedDuration + " [" + end + "]";
        }
        log.info(message);
        lastSize = currentSize;
        lastTime = now;
        batchCount = 0;
        batchTotalSize = 0;
    }

    // Used to show progress when using batches
    protected void startProcess(int amountBatches, int batchSize, int totalDocAmount) {
        log.info("Starting Processing: batches: " + amountBatches + " size: " + batchSize + " total: " + getBigNumber(totalDocAmount) + " at: " + startTime);
        lastTime = new Date();
    }

    // Used to show progress when using batches
    //  protected void progress(int currentBatch, int totalBatches, int processedAmount) {
    //      double percent = ((double) currentBatch / (double) totalBatches);
    //      Date now = new Date();
    //      long diff = now.getTime() - startTime.getTime();
    //      long time = (now.getTime() - lastTime.getTime());
    //      if (percent > 0) {
    //          int perms = (int) (diff / percent);
    //          Date end = new Date(startTime.getTime() + perms);
    //          log.info("Batch: " + currentBatch + " of " + totalBatches + " took: " + time + "ms to process " + processedAmount + " records at a rate of: " + ((processedAmount * 1000) / time) + "r/s, Memory: " + df.format(memoryPercent() * 100) + "%, Percentage complete: " + (int) (percent * 100) + "%, Estimated Finish: " + end);
    //      } else {
    //          log.info("Batch: " + currentBatch + " of " + totalBatches + " took: " + time + "ms to process " + processedAmount + " records at a rate of: " + ((processedAmount * 1000) / time) + "r/s");
    //      }
    //      lastTime = now;
    //  }

    private void finishProcess(int totalDocAmount) {
        Date now = new Date();
        long duration = now.getTime() - startTime.getTime();
        String result = getHumanReadableTimeDisplay(duration);
        log.info("Finished: took: " + result + " to process " + getBigNumber(totalDocAmount) + " records at a rate of: " + ((totalDocAmount * 1000) / duration) + "r/s");
    }

    public static String getBigNumber(int number) {
        return String.format("%,d", number);
    }

    public static String getHumanReadableTimeDisplay(long duration) {
        long hours = TimeUnit.MILLISECONDS.toHours(duration) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(duration));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void checkMemory() {
        if (memoryPercent() > 0.95) {
            log.warn("Memory Warning: " + df.format(memoryPercent() * 100) + "%");
            log.warn("Used Mem: " + (runtime.totalMemory() - runtime.freeMemory()));
            log.warn("Free Mem: " + runtime.freeMemory());
            log.warn("Total Mem: " + runtime.totalMemory());
            log.warn("Max Memory: " + runtime.maxMemory());
        }
    }

    private double memoryPercent() {
        return ((double) runtime.totalMemory() - (double) runtime.freeMemory()) / (double) runtime.maxMemory();
    }

    void initiateThreading(LinkedBlockingDeque<String> queue) throws InterruptedException {
        Integer numberOfThreads = indexerConfig.getThreadCount();

        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < numberOfThreads; i++) {
            Thread t = new Thread(new Runnable() {
                public void run() {
                    startSingleThread(queue);
                }
            });
            threads.add(t);
            t.start();
        }

        int total = queue.size();
        startProcess(total);

        while (queue.size() > 0) {
            TimeUnit.SECONDS.sleep(60);
            progress(queue.size(), total);
        }

        for (Thread t : threads) {
            t.join();
        }

        finishProcess(total);
    }

    protected abstract void startSingleThread(LinkedBlockingDeque<String> queue);

}
