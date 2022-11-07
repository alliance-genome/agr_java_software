package org.alliancegenome.indexer.indexers;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.util.StatsCollector;
import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.es.util.ProcessDisplayHelper;
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
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.xcontent.XContentType;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class Indexer extends Thread {

	public static String indexName;
	private Logger log = LogManager.getLogger(getClass());
	protected IndexerConfig indexerConfig;
	private RestHighLevelClient searchClient;
	protected Runtime runtime = Runtime.getRuntime();
	protected DecimalFormat df = new DecimalFormat("#");
	protected ObjectMapper om = new ObjectMapper();

	private ProcessDisplayHelper display = new ProcessDisplayHelper();
	private StatsCollector stats = new StatsCollector();

	protected Map<String,Double> popularityScore;

	protected BulkProcessor bulkProcessor;

	public Indexer(IndexerConfig indexerConfig) {
		this.indexerConfig = indexerConfig;

		om.setSerializationInclusion(Include.NON_NULL);
		om = customizeObjectMapper(om);

		loadPopularityScore();

		searchClient = EsClientFactory.getDefaultEsClient();

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

		BulkProcessor.Builder builder = BulkProcessor.builder((request, bulkListener) -> searchClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener);
		builder.setBulkActions(indexerConfig.getBulkActions());
		builder.setBulkSize(new ByteSizeValue(indexerConfig.getBulkSize(), ByteSizeUnit.MB));
		builder.setConcurrentRequests(indexerConfig.getConcurrentRequests());
		builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(1L), 60));

		bulkProcessor = BulkProcessor.builder((request, bulkListener) -> searchClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener).build();

	}

	private void loadPopularityScore() {

		popularityScore = new HashMap<>();

		try {
			Path popularityFile = Paths.get(ConfigHelper.getPopularityFileName());
			if (!Files.exists(popularityFile)) {
				FileUtils.copyURLToFile(new URL(ConfigHelper.getPopularityDownloadUrl()), popularityFile.toFile());
			}
			popularityScore = Files.lines(popularityFile).collect(Collectors.toMap(key -> String.valueOf(key.split("\t")[0]), val -> Double.valueOf(val.split("\t")[1])));
		} catch (IOException e) {
			e.printStackTrace();
			log.error(e.getMessage());
			System.exit(-1);
		}

	}

	public void runIndex() {
		try {
			display.startProcess(getClass().getName());
			index();
			log.info("Waiting for bulkProcessor to finish");
			bulkProcessor.flush();
			bulkProcessor.awaitClose(30L, TimeUnit.DAYS);
			display.finishProcess();
			stats.printOutput();
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			System.exit(-1);
		}
	}

	@Override
	public void run() {
		super.run();
		try {
			display.startProcess(getClass().getName());
			index();
			log.info("Waiting for bulkProcessor to finish");
			bulkProcessor.flush();
			bulkProcessor.awaitClose(30L, TimeUnit.DAYS);
			display.finishProcess();
			stats.printOutput();
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			System.exit(-1);
		}
	}

	public <D extends ESDocument> void indexDocuments(Iterable<D> docs) {
		indexDocuments(docs, null);
	}

	public <D extends ESDocument> void indexDocuments(Iterable<D> docs, Class<?> view) {
		for (D doc : docs) {
			try {
				String json = "";
				if(view != null) {
					json = om.writerWithView(view).writeValueAsString(doc);
				} else {
					json = om.writeValueAsString(doc);
				}
				display.progressProcess();
				stats.addDocument(json);
				bulkProcessor.add(new IndexRequest(indexName).source(json, XContentType.JSON));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				log.error(e.getMessage());
				System.exit(-1);
			}
		}
	}
	
	public void initiateThreading(LinkedBlockingDeque<String> queue) throws InterruptedException {
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
		
		while (queue.size() > 0) {
			TimeUnit.SECONDS.sleep(10);
		}

		for (Thread t : threads) {
			t.join();
		}
	}

	protected abstract void index();
	protected abstract void startSingleThread(LinkedBlockingDeque<String> queue);
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) { return objectMapper; }
}
