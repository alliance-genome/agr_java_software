package org.alliancegenome.indexer.indexers.curation;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.DiseaseDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.DiseaseSummaryDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class DiseaseSummaryCurationIndexer extends Indexer {

	private final DiseaseDocumentInterface diseaseApi = RestProxyFactory.createProxy(DiseaseDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	
	private HashMap<String, Object> params = new HashMap<>() {{
		put("internal", false);
		put("obsolete", false);
	}};

	public DiseaseSummaryCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index(ProcessDisplayHelper display) {
		try {
			SearchResponse<DiseaseSummaryDocument> diseaseSummaryResponse = diseaseApi.findSummary(0, 0, params);
			display.startProcess(diseaseSummaryResponse.getTotalResults());
			int totalPages = (int) (diseaseSummaryResponse.getTotalResults() / indexerConfig.getBufferSize());
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i <= totalPages; i++) {
				queue.add(String.valueOf(i));
			}
			initiateThreading(queue);
		} catch (Exception e) {
			ExceptionCatcher.report(e);
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
				SearchResponse<DiseaseSummaryDocument> response = diseaseApi.findSummary(Integer.valueOf(page), indexerConfig.getBufferSize(), params);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					return;
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
