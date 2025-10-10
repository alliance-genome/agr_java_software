package org.alliancegenome.indexer.indexers.curation;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.AlleleDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.BaseService;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class AlleleSummaryCurationIndexer extends Indexer {

	private final AlleleDocumentInterface alleleApi = RestProxyFactory.createProxy(AlleleDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final BaseService baseService = new BaseService();
	private Set<String> allNeoAlleleIDs = baseService.getAllNeoAlleleIDs();
	private HashMap<String, Object> params = new HashMap<>() {{
		put("internal", false);
		put("obsolete", false);
	}};

	public AlleleSummaryCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		try {

			SearchResponse<AlleleSummaryDocument> alleleSummaryResponse = alleleApi.findSummary(0, 0, params);
			int totalPages = (int) (alleleSummaryResponse.getTotalResults() / indexerConfig.getBufferSize());

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
				SearchResponse<AlleleSummaryDocument> response = alleleApi.findSummary(Integer.valueOf(page), indexerConfig.getBufferSize(), params);
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
