package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.converter.literature.LiteratureConverter;
import org.alliancegenome.core.document.LiteratureSearchResultDocument;
import org.alliancegenome.core.document.LiteratureSummaryDocument;
import org.alliancegenome.core.es.util.ElasticSearchInterface;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.service.ResourceDescriptorService;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class LiteratureIndexer extends Indexer {
	private String indexName = ConfigHelper.getBlueTeamESIndex();
	private ElasticSearchInterface literatureESApi = RestProxyFactory.createProxy(ElasticSearchInterface.class, ConfigHelper.getBlueTeamESUrl());
	private final ResourceDescriptorService resourceDescriptorService = new ResourceDescriptorService();
	private final AtomicBoolean emptyXrefsWarned = new AtomicBoolean(false);
	private LiteratureConverter literatureConverter = new LiteratureConverter();

	public LiteratureIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	// with multiple thread
	@Override
	protected void index(ProcessDisplayHelper display) {
		log.info("IndexName: " + indexName);
		Map<String, Object> countObject = literatureESApi.count(indexName);
		try {
			resourceDescriptorService.warm();

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
					enrichCrossReferences(sourceMap);

					LiteratureSummaryDocument doc = new LiteratureSummaryDocument();
					doc.setLiteratureSummary(sourceMap);
					list.add(doc);
				}
				
				List<LiteratureSearchResultDocument> searchResultlist = literatureConverter.convertToSearchResults(list);

				indexDocuments(searchResultlist);
				indexDocuments(list);

			} catch (Exception e) {
				ExceptionCatcher.report(e);
				throw new RuntimeException("Unhandled error for page " + page, e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void enrichCrossReferences(Map<String, Object> sourceMap) {
		if (sourceMap == null) {
			return;
		}
		Object rawCrossRefs = sourceMap.get("cross_references");
		if (!(rawCrossRefs instanceof List)) {
			if (rawCrossRefs == null && emptyXrefsWarned.compareAndSet(false, true)) {
				log.warn("LiteratureIndexer: source doc has no 'cross_references' key");
			}
			return;
		}
		for (Object entry : (List<Object>) rawCrossRefs) {
			if (!(entry instanceof Map)) {
				continue;
			}
			Map<String, Object> entryMap = (Map<String, Object>) entry;
			Object curieRaw = entryMap.get("curie");
			if (!(curieRaw instanceof String)) {
				continue;
			}
			String curie = (String) curieRaw;
			// Add a referencedCurie alias so generic CrossReference consumers can read this
			// blue-team-shaped entry without a special case.
			entryMap.put("referencedCurie", curie);
			String urlTemplate = resourceDescriptorService.resolveUrlTemplate(curie, "reference");
			if (urlTemplate != null) {
				Map<String, Object> page = new HashMap<>();
				page.put("urlTemplate", urlTemplate);
				entryMap.put("resourceDescriptorPage", page);
			}
		}
	}
}
