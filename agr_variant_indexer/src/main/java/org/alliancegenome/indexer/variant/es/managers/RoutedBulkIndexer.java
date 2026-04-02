package org.alliancegenome.indexer.variant.es.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoutedBulkIndexer extends Thread {

	private final LinkedBlockingDeque<List<byte[]>> jsonQueue;
	private final String indexName;
	private final int shardCount;
	private final long maxBulkSizeBytes;
	private final int maxRetries;
	private final long retryBaseMs;
	private final String label;

	private RestHighLevelClient client;
	private ProcessDisplayHelper phGlobal;
	private ProcessDisplayHelper ph;

	private final SummaryStatistics docStats = new SummaryStatistics();
	private final SummaryStatistics queueStats = new SummaryStatistics();
	private final SummaryStatistics esBatchRequestStats = new SummaryStatistics();

	private boolean gatherStats = VariantConfigHelper.isGatherStats();

	private long totalBytes;
	private long totalRetries;
	private long totalFailedDocs;

	public RoutedBulkIndexer(
		LinkedBlockingDeque<List<byte[]>> jsonQueue,
		String indexName,
		int shardCount,
		int maxRetries,
		String label,
		ProcessDisplayHelper phGlobal
	) {
		this.jsonQueue = jsonQueue;
		this.indexName = indexName;
		this.shardCount = shardCount;
		this.maxBulkSizeBytes = (ConfigHelper.getEsBulkSizeMB() * 1024 * 1024) * 10; //10MB * the multiplier 
		this.maxRetries = maxRetries;
		this.retryBaseMs = 1000;
		this.label = label;
		this.phGlobal = phGlobal;
	}

	@Override
	public void run() {
		boolean indexing = VariantConfigHelper.isIndexing();
		if (indexing) {
			client = EsClientFactory.getMustCloseSearchClient();
		}
		ph = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
		if(gatherStats) {
			ph.startProcess(label);
		}

		try {
			List<byte[]> pendingDocs = new ArrayList<>();
			long pendingBytes = 0;

			while (!Thread.currentThread().isInterrupted()) {
				try {
					List<byte[]> docs = jsonQueue.poll(1, TimeUnit.SECONDS);
					if (docs == null) {
						continue;
					}
					if(gatherStats) {
						queueStats.addValue(docs.size());
					}

					for (byte[] smileDoc : docs) {
						int docBytes = smileDoc.length;
						
						if(gatherStats) {
							docStats.addValue(docBytes);
						}

						if (pendingBytes + docBytes > maxBulkSizeBytes && !pendingDocs.isEmpty()) {
							submitBatch(pendingDocs);
							pendingDocs = new ArrayList<>();
							pendingBytes = 0;
						}

						pendingDocs.add(smileDoc);
						pendingBytes += docBytes;
						if(gatherStats) {
							totalBytes += docBytes;
							ph.progressProcess(
								"qs: (" + jsonQueue.size() +
								") q: (" + queueStats.getN() + "/" + (int)queueStats.getMean() +
								") d: (" + docStats.getN() + "/" + (int)docStats.getMean() +
								") es: (" + esBatchRequestStats.getN() + "/" + (int)esBatchRequestStats.getMean() +
								") B/r/f: (" + totalBytes + "/" + totalRetries + "/" + totalFailedDocs + ")"
							);
						}
						phGlobal.progressProcess();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			// Clear interrupt flag so the final bulk request can complete
			Thread.interrupted();

			// Flush remaining
			if (!pendingDocs.isEmpty()) {
				submitBatch(pendingDocs);
			}

		} finally {
			if(gatherStats) {
				ph.finishProcess();
				logStats();
			}
			if (client != null) {
				try {
					client.close();
				} catch (IOException e) {
					log.error(label + " Error closing ES client", e);
				}
			}
		}
	}

	private void submitBatch(List<byte[]> docs) {
		if(gatherStats) {
			esBatchRequestStats.addValue(docs.size());
		}
		String routing = Integer.toString(ThreadLocalRandom.current().nextInt(shardCount));
		submitWithRetry(docs, routing, 0);
	}

	private void submitWithRetry(List<byte[]> docs, String routing, int attempt) {
		if (!VariantConfigHelper.isIndexing()) {
			return;
		}
		BulkRequest bulkRequest = new BulkRequest();
		for (byte[] smileDoc : docs) {
			bulkRequest.add(new IndexRequest(indexName).source(smileDoc, XContentType.SMILE).routing(routing));
		}

		try {
			BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);

			if (response.hasFailures()) {
				List<byte[]> failedDocs = new ArrayList<>();
				for (BulkItemResponse item : response) {
					if (item.isFailed()) {
						failedDocs.add(docs.get(item.getItemId()));
					}
				}

				if (!failedDocs.isEmpty() && attempt < maxRetries) {
					totalRetries++;
					long sleepMs = retryBaseMs * (1L << Math.min(attempt, 10));
					log.warn(label + " Retrying " + failedDocs.size() + " failed items (attempt " + (attempt + 1) + "), sleeping " + sleepMs + "ms");
					try {
						Thread.sleep(sleepMs);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
					submitWithRetry(failedDocs, routing, attempt + 1);
				} else if (!failedDocs.isEmpty()) {
					totalFailedDocs += failedDocs.size();
					log.error(label + " Failed to index " + failedDocs.size() + " documents after " + maxRetries + " retries");
					log.error(label + " First failure: " + response.getItems()[0].getFailureMessage());
				}
			}

		} catch (IOException e) {
			if (attempt < maxRetries) {
				totalRetries++;
				long sleepMs = retryBaseMs * (1L << Math.min(attempt, 10));
				log.warn(label + " Bulk request failed: " + e.getMessage() + ", retrying (attempt " + (attempt + 1) + "), sleeping " + sleepMs + "ms");
				try {
					Thread.sleep(sleepMs);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					return;
				}
				submitWithRetry(docs, routing, attempt + 1);
			} else {
				log.error(label + " Bulk request failed after " + maxRetries + " retries: " + e.getMessage());
				System.exit(-1);
			}
		}
	}

	private void logStats() {
		log.info(label + " Doc Stats: " + docStats);
		log.info(label + " Queue Stats: " + queueStats);
		log.info(label + " ES Batch Stats: " + esBatchRequestStats);
		log.info(label + " Total Bytes: " + totalBytes + " Retries: " + totalRetries + " Failed: " + totalFailedDocs);
	}
}
