package org.alliancegenome.indexer.variant.es.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.GeneDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.GeneSummaryDocument;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.core.config.RestConfig;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class GeneCacheService {

	private static final int BATCH_SIZE = 1000;
	private static final int THREAD_COUNT = 8;

	public Map<String, Gene> loadGeneCache() {
		log.info("Fetching gene IDs from curation API...");
		GeneDocumentInterface geneApi = RestProxyFactory.createProxy(GeneDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
		SearchResponse<Long> idsResponse = geneApi.getAllIds();
		List<Long> allIds = idsResponse.getResults();
		log.info("Fetched {} gene IDs", allIds.size());

		List<List<Long>> batches = new ArrayList<>();
		for (int i = 0; i < allIds.size(); i += BATCH_SIZE) {
			batches.add(new ArrayList<>(allIds.subList(i, Math.min(i + BATCH_SIZE, allIds.size()))));
		}

		LinkedBlockingDeque<List<Long>> queue = new LinkedBlockingDeque<>(batches);
		Map<String, Gene> geneCacheMap = new ConcurrentHashMap<>();

		ProcessDisplayHelper ph = new ProcessDisplayHelper();
		ph.startProcess("Loading Gene Cache", allIds.size());

		log.info("Loading gene cache: {} batches of {} across {} threads", batches.size(), BATCH_SIZE, THREAD_COUNT);
		List<Thread> threads = new ArrayList<>();
		for (int t = 0; t < THREAD_COUNT; t++) {
			Thread thread = new Thread(() -> {
				GeneDocumentInterface threadApi = RestProxyFactory.createProxy(GeneDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
				while (true) {
					List<Long> batch = queue.poll();
					if (batch == null) {
						return;
					}
					SearchResponse<GeneSummaryDocument> response = threadApi.findCacheByIds(batch);
					if (response != null && response.getResults() != null) {
						for (GeneSummaryDocument doc : response.getResults()) {
							Gene gene = doc.getGene();
							if (gene != null && gene.getPrimaryExternalId() != null) {
								geneCacheMap.put(gene.getPrimaryExternalId(), gene);
							}
							ph.progressProcess();
						}
					}
				}
			});
			thread.start();
			threads.add(thread);
		}

		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		ph.finishProcess();
		log.info("Gene cache loaded: {} genes", geneCacheMap.size());

		return geneCacheMap;
	}

}
