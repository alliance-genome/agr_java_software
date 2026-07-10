package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.document.LiteratureSummaryDocument;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.es.util.ElasticSearchInterface;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class LiteratureIndexer extends Indexer {
	private String indexName = ConfigHelper.getBlueTeamESIndex();
	private ElasticSearchInterface literatureESApi = RestProxyFactory.createProxy(ElasticSearchInterface.class, ConfigHelper.getBlueTeamESUrl());

	public LiteratureIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	// with multiple thread
	@Override
	protected void index(ProcessDisplayHelper display) {
		log.info("IndexName: " + indexName);
		Map<String, Object> countObject = literatureESApi.count(indexName);
		try {

			int totalPages = (int) countObject.get("count") / indexerConfig.getBufferSize();
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i <= totalPages; i++) {
				queue.add(String.valueOf(i));
			}

			log.info("total pages:" + totalPages);
			initiateThreading(queue);

		} catch (InterruptedException e) {
			e.printStackTrace();
			ExceptionCatcher.report(e);
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		while (!queue.isEmpty()) {
			String page = null;
			try {
				page = queue.takeFirst();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}

			try {
				int from = Integer.parseInt(page) * indexerConfig.getBufferSize();
				Map<String, Object> object = literatureESApi.search(indexName, from, indexerConfig.getBufferSize());
				Map<String, Object> hitsMap = (Map<String, Object>) object.get("hits");
				List<Map<String, Object>> hits = (List<Map<String, Object>>) hitsMap.get("hits");
				List<LiteratureSummaryDocument> list = new ArrayList<>();

				for (Map<String, Object> map : hits) {
					Map<String, Object> sourceMap = (Map<String, Object>) map.get("_source");

					LiteratureSummaryDocument doc = new LiteratureSummaryDocument();
					doc.setLiteratureSummary(sourceMap);
					list.add(doc);
				}

				indexDocuments(list);

			} catch (Exception e) {
				ExceptionCatcher.report(e);
				throw new RuntimeException("Unhandled error for page " + page, e);
			}
		}
	}


}
