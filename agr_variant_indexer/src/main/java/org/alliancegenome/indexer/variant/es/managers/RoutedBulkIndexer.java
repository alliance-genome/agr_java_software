package org.alliancegenome.indexer.variant.es.managers;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.http.ConnectionClosedException;
import org.apache.http.conn.ConnectTimeoutException;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.xcontent.XContentType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoutedBulkIndexer extends Thread {

	private final LinkedBlockingDeque<List<byte[]>> jsonQueue;
	private final String indexName;
	private final long maxBulkSizeBytes;
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
		String label,
		ProcessDisplayHelper phGlobal
	) {
		this.jsonQueue = jsonQueue;
		this.indexName = indexName;
		this.maxBulkSizeBytes = ConfigHelper.getEsBulkSizeMB() * 1024 * 1024;
		this.label = label;
		this.phGlobal = phGlobal;
	}

	@Override
	public void run() {
		boolean indexing = VariantConfigHelper.isIndexing();
		if (indexing) {
			client = EsClientFactory.getMustCloseSearchClient();
			log.info(label + " ES client created: " + System.identityHashCode(client));
		}
		ph = new ProcessDisplayHelper(VariantConfigHelper.getDisplayInterval());
		if (gatherStats) {
			ph.startProcess(label);
		}

		try {
			List<byte[]> pendingDocs = new ArrayList<>();
			long pendingBytes = 0;

			while (!Thread.currentThread().isInterrupted()) {
				try {
					List<byte[]> docs = jsonQueue.take();

					if (gatherStats) {
						queueStats.addValue(docs.size());
					}

					for (byte[] smileDoc : docs) {
						int docBytes = smileDoc.length;

						if (gatherStats) {
							docStats.addValue(docBytes);
						}

						if (pendingBytes + docBytes > maxBulkSizeBytes && !pendingDocs.isEmpty()) {
							submitBatch(pendingDocs);
							pendingDocs = new ArrayList<>();
							pendingBytes = 0;
						}

						pendingDocs.add(smileDoc);
						pendingBytes += docBytes;
						if (gatherStats) {
							totalBytes += docBytes;
							ph.progressProcess(
								"qs: (" + jsonQueue.size()
								+ ") q: (" + queueStats.getN() + "/" + (int) queueStats.getMean()
								+ ") d: (" + docStats.getN() + "/" + (int) docStats.getMean()
								+ ") es: (" + esBatchRequestStats.getN() + "/" + (int) esBatchRequestStats.getMean()
								+ ") B/r/f: (" + totalBytes + "/" + totalRetries + "/" + totalFailedDocs + ")"
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

		} catch (Exception e) {
			log.error(label + " Unexpected exception in indexer thread, exiting", e);
			e.printStackTrace();
			System.exit(-1);
		} finally {
			if (gatherStats) {
				ph.finishProcess();
				logStats();
			}
			if (client != null) {
				try {
					log.info(label + " Closing ES client: " + System.identityHashCode(client));
					client.close();
					log.info(label + " ES client closed: " + System.identityHashCode(client));
				} catch (IOException e) {
					log.error(label + " Error closing ES client: " + System.identityHashCode(client), e);
				}
			}
		}
	}

	private void submitBatch(List<byte[]> docs) {
		if (gatherStats) {
			esBatchRequestStats.addValue(docs.size());
		}
		String routing = Integer.toString(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));
		submitWithRetry(docs, routing);
	}

	private void submitWithRetry(List<byte[]> docs, String routing) {
		if (!VariantConfigHelper.isIndexing()) {
			return;
		}
		BulkRequest bulkRequest = new BulkRequest().timeout(TimeValue.timeValueHours(1));
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
				if (!failedDocs.isEmpty()) {
					totalRetries++;
					log.warn(label + " " + failedDocs.size() + " failed items, splitting and requeueing");
					requeueSplit(failedDocs);
				}
			}

		} catch (ElasticsearchStatusException e) {
			totalRetries++;
			log.warn(label + " Bulk request rejected (HTTP " + e.status().getStatus() + "): " + e.getMessage() + ", sleeping and splitting " + docs.size() + " items and requeueing");
			try {
				Thread.sleep(1000 + ThreadLocalRandom.current().nextInt(2000));
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
			requeueSplit(docs);
		} catch (ConnectTimeoutException e) {
			totalRetries++;
			log.warn(label + " Bulk request ConnectTimeoutException: " + e.getMessage() + ", reconnecting and splitting " + docs.size() + " items and requeueing");
			reconnectClient();
			requeueSplit(docs);
		} catch (SocketTimeoutException e) {
			totalRetries++;
			log.warn(label + " Bulk request SocketTimeoutException: " + e.getMessage() + ", reconnecting and splitting " + docs.size() + " items and requeueing");
			reconnectClient();
			requeueSplit(docs);
		} catch (ConnectionClosedException e) {
			totalRetries++;
			log.warn(label + " Bulk request ConnectionClosedException: " + e.getMessage() + ", reconnecting and splitting " + docs.size() + " items and requeueing");
			reconnectClient();
			requeueSplit(docs);
		} catch (ConnectException e) {
			totalRetries++;
			log.warn(label + " Bulk request ConnectException: " + e.getMessage() + ", reconnecting and splitting " + docs.size() + " items and requeueing");
			reconnectClient();
			requeueSplit(docs);
		} catch (IOException e) {
			totalRetries++;
			log.warn(label + " Bulk request IOException: " + e.getMessage() + ", reconnecting and splitting " + docs.size() + " items and requeueing");
			reconnectClient();
			requeueSplit(docs);
		} catch (RuntimeException e) {
			totalRetries++;
			log.warn(label + " Bulk request RuntimeException (" + e.getClass().getSimpleName() + "): " + e.getMessage() + ", reconnecting and splitting " + docs.size() + " items and requeueing");
			reconnectClient();
			requeueSplit(docs);
		}
	}

	private void reconnectClient() {
		try {
			log.info(label + " Closing dead ES client: " + System.identityHashCode(client));
			client.close();
		} catch (IOException ce) {
			log.warn(label + " Error closing dead client: " + ce.getMessage());
		}
		client = EsClientFactory.getMustCloseSearchClient();
		log.info(label + " ES client reconnected: " + System.identityHashCode(client));
	}

	private void requeueSplit(List<byte[]> docs) {
		try {
			if (docs.size() == 1) {
				jsonQueue.put(docs);
			} else {
				int mid = docs.size() / 2;
				jsonQueue.put(new ArrayList<>(docs.subList(0, mid)));
				jsonQueue.put(new ArrayList<>(docs.subList(mid, docs.size())));
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void logStats() {
		log.info(label + " Doc Stats: " + docStats);
		log.info(label + " Queue Stats: " + queueStats);
		log.info(label + " ES Batch Stats: " + esBatchRequestStats);
		log.info(label + " Total Bytes: " + totalBytes + " Retries: " + totalRetries + " Failed: " + totalFailedDocs);
	}
}
