package org.alliancegenome.indexer.indexers.curation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.api.entity.GeneExpressionExperimentDocument;
import org.alliancegenome.curation_api.model.entities.GeneExpressionExperiment;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.GeneExpressionExperimentService;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class GeneExpressionExperimentIndexer extends Indexer {
	GeneExpressionExperimentService geneExpressionExperimentService;

	public GeneExpressionExperimentIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		geneExpressionExperimentService = new GeneExpressionExperimentService();
		try {
			SearchResponse<GeneExpressionExperiment> response = geneExpressionExperimentService.getGeneExpressionExperiments(0, 0);
			log.info("GeneExpressionExperiment count: " + response.getTotalResults());
			int totalPages = (int) (response.getTotalResults() / indexerConfig.getBufferSize());
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i <= totalPages; i++) {
				queue.add(String.valueOf(i));
			}
			initiateThreading(queue);
		} catch (InterruptedException e) {
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
				log.debug(queue.size() + " pages to process " + Thread.currentThread().getName() + " starting page: " + page);
				SearchResponse<GeneExpressionExperiment> response = geneExpressionExperimentService.getGeneExpressionExperiments(Integer.valueOf(page), indexerConfig.getBufferSize());
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					return;
				}
				List<GeneExpressionExperimentDocument> documentsToIndex = new ArrayList<>();
				for (GeneExpressionExperiment gee : response.getResults()) {
					GeneExpressionExperimentDocument geneExpressionExperimentDocument = new GeneExpressionExperimentDocument();
					geneExpressionExperimentDocument.setGeneExpressionExperiment(gee);
					documentsToIndex.add(geneExpressionExperimentDocument);
				}
				indexDocuments(documentsToIndex);
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
