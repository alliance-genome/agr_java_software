package org.alliancegenome.indexer.processors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.curation_api.util.ProcessDisplayHelper;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.kmeans.KMeans;
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
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.core.TimeValue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ESDocumentProcessor {

	private String indexName;
	private RestHighLevelClient searchClient;

	private Integer clusterCount = 10;

	private BulkProcessor.Listener listener = new BulkProcessor.Listener() {
		@Override
		public void beforeBulk(long executionId, BulkRequest request) {
		}

		@Override
		public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
			// log.info("Size: " + request.requests().size() + " MB: " +
			// request.estimatedSizeInBytes() + " Time: " + response.getTook() + " Bulk
			// Requet Finished");
		}

		@Override
		public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
			log.error("Bulk Request Failure: " + failure.getMessage());
			for (DocWriteRequest<?> req : request.requests()) {
				// IndexRequest idxreq = (IndexRequest)req;
				// bulkProcessor.add(idxreq);
			}
			log.error("Finished Adding failed requests to bulkProcessor: ");
		}
	};

	public ESDocumentProcessor(String indexName) {
		this.indexName = indexName;
		searchClient = EsClientFactory.getMustCloseSearchClient();
	}

	public void processIndexes() {

		ArrayList<Integer> list = new ArrayList<>();
		
		log.info("Reading Data Files");
		for (IndexerConfig config : IndexerConfig.values()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(new File("/data/" + config.getIndexClazz().getSimpleName() + "_data.json")));
				String line = null;
				while ((line = reader.readLine()) != null) {
					list.add(line.length());
				}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			log.info("Finished reading data file: " + config.getIndexClazz().getSimpleName());
		}

		log.info("Computing KMeans");
		KMeans kMeans = new KMeans(clusterCount, 500, list);
		kMeans.run();
		log.info("KMeans Centers: " + kMeans.getCenters());

		int previousCenter = 0;

		NavigableMap<Integer, BulkProcessor> bulkProcessorsMap = new TreeMap<Integer, BulkProcessor>();

		log.info("Creating Bulk Processors");
		for (Integer center : kMeans.getCenters()) {
			log.info("Center: " + center);
			int mid = 0;
			if (previousCenter != 0) {
				mid = ((center - previousCenter) / 2) + previousCenter;
			}

			BulkProcessor.Builder builder = BulkProcessor.builder((request, bulkListener) -> searchClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener), listener, "Processor for: " + center);
			ByteSizeValue m10 = new ByteSizeValue(10, ByteSizeUnit.MB);
			log.info("Action Size: " + ((int)(m10.getBytes() / center)));
			builder.setBulkActions((int)(m10.getBytes() / center));
			builder.setBulkSize(m10);
			builder.setConcurrentRequests(10);
			builder.setBackoffPolicy(BackoffPolicy.exponentialBackoff(TimeValue.timeValueSeconds(10L), 60));

			bulkProcessorsMap.put(mid, builder.build());

			previousCenter = center;
		}
		
		log.info("Adding documents to BulkProcessors");
		for (IndexerConfig config : IndexerConfig.values()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(new File("/data/" + config.getIndexClazz().getSimpleName() + "_data.json")));
				String line = null;
				ProcessDisplayHelper ph = new ProcessDisplayHelper(2000);
				ph.startProcess(config.getIndexClazz().getSimpleName() + " processor starting");
				while ((line = reader.readLine()) != null) {
					bulkProcessorsMap.floorEntry(line.length()).getValue().add(new IndexRequest(indexName).source(line, XContentType.JSON));
					ph.progressProcess();
				}
				ph.finishProcess();
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			log.info("Indexer: " + config.getIndexClazz().getSimpleName());
		}
		
		log.info("Waiting for BulkProcessor to finished");
		for(Entry<Integer, BulkProcessor> entry: bulkProcessorsMap.entrySet()) {
			entry.getValue().flush();
			try {
				entry.getValue().awaitClose(30L, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.info("Everything is finished");
	}

	public void close() {
		try {
			searchClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
