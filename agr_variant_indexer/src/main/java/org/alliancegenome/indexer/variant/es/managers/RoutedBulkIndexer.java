package org.alliancegenome.indexer.variant.es.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.es.util.EsClientFactory;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.variant.config.VariantConfigHelper;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.elasticsearch.client.RestHighLevelClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoutedBulkIndexer extends Thread {

	private final LinkedBlockingDeque<List<byte[]>> jsonQueue;
	private final String indexName;
	private final long maxBulkSizeBytes;
	private final String label;
	private final RetryWorker retryWorker;

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
		ProcessDisplayHelper phGlobal,
		RetryWorker retryWorker
	) {
		this.jsonQueue = jsonQueue;
		this.indexName = indexName;
		this.maxBulkSizeBytes = ConfigHelper.getEsBulkSizeMB() * 1024 * 1024;
		this.label = label;
		this.phGlobal = phGlobal;
		this.retryWorker = retryWorker;
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
		if (!VariantConfigHelper.isIndexing()) {
			return;
		}
		if (gatherStats) {
			esBatchRequestStats.addValue(docs.size());
		}
		String routing = Integer.toString(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));
		BulkSubmitResult result = BulkSubmitter.submitOnce(docs, client, routing, indexName);

		switch (result.getStatus()) {
			case SUCCESS:
				return;
			case PARTIAL_FAILURE:
				totalRetries++;
				log.warn(label + " " + result.getFailedDocs().size() + " failed items, handing to retry worker");
				handOff(result.getFailedDocs());
				return;
			case FULL_FAILURE:
				totalRetries++;
				log.warn(label + " Bulk request failed (" + result.getReason() + "), size=" + docs.size() + (result.isReconnectNeeded() ? ", reconnecting" : "") + (result.isSleepNeeded() ? ", sleeping" : "") + ", handing to retry worker");
				if (result.isSleepNeeded()) {
					try {
						Thread.sleep(1000 + ThreadLocalRandom.current().nextInt(2000));
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
				}
				if (result.isReconnectNeeded()) {
					reconnectClient();
				}
				handOff(docs);
				return;
		}
	}

	private void handOff(List<byte[]> docs) {
		try {
			retryWorker.submit(docs);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
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

	private void logStats() {
		log.info(label + " Doc Stats: " + docStats);
		log.info(label + " Queue Stats: " + queueStats);
		log.info(label + " ES Batch Stats: " + esBatchRequestStats);
		log.info(label + " Total Bytes: " + totalBytes + " Retries: " + totalRetries + " Failed: " + totalFailedDocs);
	}
}
