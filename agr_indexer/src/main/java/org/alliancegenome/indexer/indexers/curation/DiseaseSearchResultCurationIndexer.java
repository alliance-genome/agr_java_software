package org.alliancegenome.indexer.indexers.curation;

import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.DiseaseDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.DiseaseSearchResultDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class DiseaseSearchResultCurationIndexer extends Indexer {

	private final DiseaseDocumentInterface diseaseApi = RestProxyFactory.createProxy(DiseaseDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public DiseaseSearchResultCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		try {
			log.info("Fetching all disease search result documents...");
			SearchResponse<DiseaseSearchResultDocument> response = diseaseApi.findAll();
			log.info("Fetched {} disease search result documents", response.getResults().size());

			indexDocuments(response.getResults());
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			ExceptionCatcher.report(e);
			System.exit(-1);
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		// Not used — single-shot indexing
	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}

}
