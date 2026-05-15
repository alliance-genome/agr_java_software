package org.alliancegenome.indexer.variant.es.managers;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.alliancegenome.indexer.variant.config.VariantConfigHelper;
import org.alliancegenome.es.util.EsClientFactory;
import org.elasticsearch.client.RestHighLevelClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RetryWorker extends Thread {

	static final int MAX_SPLIT_DEPTH = 7;

	private final BlockingQueue<RetryItem> queue;
	private final Deque<RetryItem> internal = new ArrayDeque<>();
	private final AtomicBoolean draining = new AtomicBoolean(false);
	private final String indexName;
	private final String label = "RetryWorker";

	private RestHighLevelClient client;

	private long totalRetries;
	private long totalFailedDocs;

	public RetryWorker(int capacity, String indexName) {
		this.queue = new LinkedBlockingQueue<>(capacity);
		this.indexName = indexName;
		setName("variant-retry-worker");
	}

	public void submit(List<byte[]> docs) throws InterruptedException {
		queue.put(new RetryItem(docs));
	}

	public void shutdown() {
		draining.set(true);
	}

	@Override
	public void run() {
		boolean indexing = VariantConfigHelper.isIndexing();
		if (indexing) {
			client = EsClientFactory.getMustCloseSearchClient();
			log.info(label + " ES client created: " + System.identityHashCode(client));
		}

		try {
			while (!draining.get() || !queue.isEmpty() || !internal.isEmpty()) {
				RetryItem item;
				if (!internal.isEmpty()) {
					item = internal.pop();
				} else {
					try {
						item = queue.poll(1, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						continue;
					}
				}
				if (item == null) {
					continue;
				}
				process(item);
			}
		} finally {
			if (client != null) {
				try {
					log.info(label + " Closing ES client: " + System.identityHashCode(client));
					client.close();
					log.info(label + " ES client closed: " + System.identityHashCode(client));
				} catch (IOException e) {
					log.warn(label + " Error closing ES client: " + e.getMessage());
				}
			}
			log.info(label + " shutdown, Retries: " + totalRetries + " Failed: " + totalFailedDocs);
		}
	}

	private void process(RetryItem item) {
		if (!VariantConfigHelper.isIndexing()) {
			return;
		}

		String routing = Integer.toString(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));
		BulkSubmitResult result = BulkSubmitter.submitOnce(item.getDocs(), client, routing, indexName);

		switch (result.getStatus()) {
			case SUCCESS:
				return;
			case PARTIAL_FAILURE:
				totalRetries++;
				item.setDocs(result.getFailedDocs());
				item.setRetryCount(item.getRetryCount() + 1);
				item.setLastFailureReason(result.getReason());
				log.warn(label + " " + item.getDocs().size() + " failed items (retryCount=" + item.getRetryCount() + ", splitDepth=" + item.getSplitDepth() + "): " + item.getLastFailureReason());
				splitAndRequeue(item);
				return;
			case FULL_FAILURE:
				totalRetries++;
				item.setRetryCount(item.getRetryCount() + 1);
				item.setLastFailureReason(result.getReason());
				log.warn(label + " Bulk request failed (retryCount=" + item.getRetryCount() + ", splitDepth=" + item.getSplitDepth() + ", size=" + item.getDocs().size() + "): " + item.getLastFailureReason());
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
				splitAndRequeue(item);
				return;
		}
	}

	private void splitAndRequeue(RetryItem item) {
		if (item.getDocs().size() == 1) {
			totalFailedDocs++;
			log.error(label + " Single document cannot be retried further (retryCount=" + item.getRetryCount() + ", splitDepth=" + item.getSplitDepth() + "): " + item.getLastFailureReason() + ", exiting");
			//System.exit(-1);
			return;
		}

		int newDepth = item.getSplitDepth() + 1;
		if (newDepth > MAX_SPLIT_DEPTH) {
			totalFailedDocs += item.getDocs().size();
			log.error(label + " MAX_SPLIT_DEPTH (" + MAX_SPLIT_DEPTH + ") exceeded at size=" + item.getDocs().size() + " (retryCount=" + item.getRetryCount() + "): " + item.getLastFailureReason() + ", exiting");
			//System.exit(-1);
			return;
		}

		int mid = item.getDocs().size() / 2;
		List<byte[]> secondHalf = new ArrayList<>(item.getDocs().subList(mid, item.getDocs().size()));
		List<byte[]> firstHalf = new ArrayList<>(item.getDocs().subList(0, mid));
		item.setDocs(firstHalf);
		item.setSplitDepth(newDepth);
		internal.push(item);
		internal.push(new RetryItem(secondHalf, newDepth));
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
}
