package org.alliancegenome.indexer.variant.es.managers;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadLocalRandom;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.config.VariantConfigHelper;
import org.alliancegenome.core.es.util.EsClientFactory;
import org.alliancegenome.core.es.util.IndexName;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.elasticsearch.client.RestHighLevelClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoutedBulkIndexer extends Thread {

	private final LinkedBlockingDeque<IndexBatch> jsonQueue;
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
		LinkedBlockingDeque<IndexBatch> jsonQueue,
		String label,
		ProcessDisplayHelper phGlobal,
		RetryWorker retryWorker
	) {
		this.jsonQueue = jsonQueue;
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
			Map<IndexName, IndexBatch> pending = new EnumMap<>(IndexName.class);

			while (!Thread.currentThread().isInterrupted()) {
				try {
					IndexBatch batch = jsonQueue.take();
					IndexName indexName = batch.getIndexName();
					IndexBatch pendingBatch = pending.computeIfAbsent(indexName, IndexBatch::new);

					if (gatherStats) {
						queueStats.addValue(batch.getDocs().size());
					}

					for (byte[] smileDoc : batch.getDocs()) {
						int docBytes = smileDoc.length;

						if (gatherStats) {
							docStats.addValue(docBytes);
						}

						if (pendingBatch.getBytes() + docBytes > maxBulkSizeBytes && !pendingBatch.getDocs().isEmpty()) {
							submitBatch(pendingBatch);
							pendingBatch = new IndexBatch(indexName);
							pending.put(indexName, pendingBatch);
						}

						pendingBatch.add(smileDoc);
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
			for (IndexBatch pendingBatch : pending.values()) {
				if (!pendingBatch.getDocs().isEmpty()) {
					submitBatch(pendingBatch);
				}
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

	private void submitBatch(IndexBatch batch) {
		if (!VariantConfigHelper.isIndexing()) {
			return;
		}
		IndexName indexName = batch.getIndexName();
		List<byte[]> docs = batch.getDocs();
		if (gatherStats) {
			esBatchRequestStats.addValue(docs.size());
		}
		String routing = Integer.toString(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));
		BulkSubmitResult result = BulkSubmitter.submitOnce(docs, client, routing, indexName.getIndexName());

		switch (result.getStatus()) {
			case SUCCESS:
				return;
			case PARTIAL_FAILURE:
				totalRetries++;
				log.warn(label + " " + indexName + " " + result.getFailedDocs().size() + " failed items, handing to retry worker");
				handOff(indexName, result.getFailedDocs());
				return;
			case FULL_FAILURE:
				totalRetries++;
				log.warn(label + " " + indexName + " Bulk request failed (" + result.getReason() + "), size=" + docs.size() + (result.isReconnectNeeded() ? ", reconnecting" : "") + (result.isSleepNeeded() ? ", sleeping" : "") + ", handing to retry worker");
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
				handOff(indexName, docs);
				return;
		}
	}

	private void handOff(IndexName indexName, List<byte[]> docs) {
		try {
			retryWorker.submit(indexName, docs);
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
