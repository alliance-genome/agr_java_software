package org.alliancegenome.indexer.indexers;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.util.ListUtils;
import org.alliancegenome.curation_api.interfaces.document.GODocumentInterface;
import org.alliancegenome.curation_api.model.document.es.GOSearchResultDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.core.config.RestConfig;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class GOSearchResultCurationIndexer extends Indexer {

	private final GODocumentInterface goApi = RestProxyFactory.createProxy(GODocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private List<List<Long>> idBatches;

	public GOSearchResultCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index(ProcessDisplayHelper display) {
		try {
			log.info("Fetching all GO search result IDs...");
			SearchResponse<Long> idsResponse = goApi.getAllIds();
			List<Long> allIds = idsResponse.getResults();
			log.info("Fetched {} GO search result IDs", allIds.size());
			display.startProcess(allIds.size());
			idBatches = ListUtils.partition(allIds, indexerConfig.getBufferSize());
			log.info("Partitioned into {} batches of up to {}", idBatches.size(), indexerConfig.getBufferSize());

			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i < idBatches.size(); i++) {
				queue.add(String.valueOf(i));
			}

			initiateThreading(queue);
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			ExceptionCatcher.report(e);
			System.exit(-1);
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		while (true) {
			try {
				if (queue.isEmpty()) {
					return;
				}
				String batchIndex = queue.takeFirst();
				List<Long> batchIds = idBatches.get(Integer.parseInt(batchIndex));

				SearchResponse<GOSearchResultDocument> response = goApi.findByIds(batchIds);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					continue;
				}

				indexDocuments(response.getResults());
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				ExceptionCatcher.report(e);
				System.exit(-1);
				return;
			}
		}
	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}
}
