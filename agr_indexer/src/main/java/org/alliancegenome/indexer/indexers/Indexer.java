package org.alliancegenome.indexer.indexers;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.core.util.StatsCollector;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.config.IndexerConfig;
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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Indexer extends Thread {

	public static String indexName;
	protected IndexerConfig indexerConfig;
	private RestHighLevelClient searchClient;
	protected Runtime runtime = Runtime.getRuntime();
	protected DecimalFormat df = new DecimalFormat("#");
	protected ObjectMapper om = new ObjectMapper();

	private ProcessDisplayHelper display = new ProcessDisplayHelper();
	private StatsCollector stats = new StatsCollector();

	protected Map<String, Double> popularityScore = new HashMap<>();

	@Getter
	private Duration duration = Duration.ZERO;

	protected BulkProcessor bulkProcessor;

	public Indexer(IndexerConfig indexerConfig) {
		this.indexerConfig = indexerConfig;

		om.setSerializationInclusion(Include.NON_NULL);
		om = customizeObjectMapper(om);

		searchClient = EsClientFactory.getDefaultEsClient();

		BulkProcessor.Listener listener = new BulkProcessor.Listener() {
			@Override
			public void beforeBulk(long executionId, BulkRequest request) {
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
				if (response.hasFailures()) {
					log.info("Size: " + request.requests().size() + " MB: " + request.estimatedSizeInBytes() + " Time: " + response.getTook() + " Bulk Request Finished");
					log.info(response.buildFailureMessage());
				}
			}

			@Override
			public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
				log.error("Bulk Request Failure: " + failure.getMessage());
				for (DocWriteRequest<?> req : request.requests()) {
					IndexRequest idxreq = (IndexRequest) req;
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
		bulkProcessor = builder.build();
		// bulkProcessor = BulkProcessor.builder((request, bulkListener) ->
		// searchClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
		// listener).build();

	}

	public void runIndex() {
		try {
			Instant start = Instant.now();
			display.startProcess(getClass().getSimpleName());
			index();
			log.info("Waiting for bulkProcessor to finish");
			bulkProcessor.flush();
			bulkProcessor.awaitClose(30L, TimeUnit.DAYS);
			display.finishProcess();
			stats.printOutput();
			Instant end = Instant.now();
			duration = Duration.between(start, end);
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
			Instant start = Instant.now();
			display.startProcess(getClass().getSimpleName());
			index();
			log.info("Waiting for bulkProcessor to finish");
			bulkProcessor.flush();
			bulkProcessor.awaitClose(30L, TimeUnit.DAYS);
			display.finishProcess();
			stats.printOutput();
			Instant end = Instant.now();
			duration = Duration.between(start, end);
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
			indexDocument(doc, view);
		}
	}

	public <D extends ESDocument> void indexDocument(D doc) {
		indexDocument(doc, null);
	}

	public <D extends ESDocument> void indexDocument(D doc, Class<?> view) {
		try {
			String json = "";
			if (view != null) {
				json = om.writerWithView(view).writeValueAsString(doc);
			} else {
				json = om.writeValueAsString(doc);
			}
			if (json.length() > 19_000_000) {
				log.error("Document is too large for ES skipping: " + json.length());
				return;
			}
			stats.addDocument(json);
			bulkProcessor.add(new IndexRequest(indexName).source(json, XContentType.JSON));
			display.progressProcess();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			log.error(e.getMessage());
			System.exit(-1);
		}
	}

	public void initiateThreading(LinkedBlockingDeque<String> queue) throws InterruptedException {
		Integer numberOfThreads = indexerConfig.getThreadCount();

		List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0; i < numberOfThreads; i++) {
			Thread t = new Thread(new Runnable() {
				@Override
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

	protected <T> List<List<T>> partition(List<T> list, int size) {
		List<List<T>> parts = new ArrayList<>();
		if (list != null) {
			for (int i = 0; i < list.size(); i += size) {
				parts.add(new ArrayList<>(list.subList(i, Math.min(i + size, list.size()))));
			}
		}
		return parts;
	}

	protected abstract void index();

	protected abstract void startSingleThread(LinkedBlockingDeque<String> queue);

	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return objectMapper;
	}
}
