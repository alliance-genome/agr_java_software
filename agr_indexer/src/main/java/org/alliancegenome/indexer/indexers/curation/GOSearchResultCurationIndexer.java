package org.alliancegenome.indexer.indexers.curation;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.GODocumentInterface;
import org.alliancegenome.curation_api.model.document.es.GOSearchResultDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class GOSearchResultCurationIndexer extends Indexer {

	private final GODocumentInterface goApi = RestProxyFactory.createProxy(GODocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private HashMap<String, Object> params = new HashMap<String, Object>() {
		{
			put("internal", false);
			put("obsolete", false);
		}
	};

	public GOSearchResultCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		try {
			SearchResponse<GOSearchResultDocument> response = goApi.findSearchResult(0, 0, params);
			int totalPages = (int) (response.getTotalResults() / indexerConfig.getBufferSize());
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i <= totalPages; i++) {
				queue.add(String.valueOf(i));
			}
			initiateThreading(queue);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		while (true) {
			try {
				if (queue.isEmpty()) {
					return;
				}
				String page = queue.takeFirst();
				SearchResponse<GOSearchResultDocument> response = goApi.findSearchResult(Integer.valueOf(page), indexerConfig.getBufferSize(), params);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					return;
				}

				indexDocuments(response.getResults());
			} catch (Exception e) {
				log.error("Error while indexing...", e);
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
