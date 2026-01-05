package org.alliancegenome.indexer.indexers.curation;

import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.GeneExpressionRibbonDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionRibbonSummaryDocument;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class GeneExpressionRibbonSummaryIndexer extends Indexer {
	private final GeneExpressionRibbonDocumentInterface api = RestProxyFactory.createProxy(GeneExpressionRibbonDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public GeneExpressionRibbonSummaryIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		try {
			GeneExpressionRibbonSummaryDocument response = api.findDocument();
			if (response == null) {
				return;
			}
			indexDocument(response);
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			System.exit(-1);
			return;
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {

	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}

}
