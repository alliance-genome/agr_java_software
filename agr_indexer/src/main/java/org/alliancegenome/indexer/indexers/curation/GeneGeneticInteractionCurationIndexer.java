package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.GeneGeneticInteractionDocument;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.GeneGeneticInteractionDocumentInterface;
import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.GeneGeneticInteractionService;
import org.alliancegenome.neo4j.view.PublicView;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class GeneGeneticInteractionCurationIndexer extends Indexer {

	private final GeneGeneticInteractionDocumentInterface geneGeneticInteractionApi = RestProxyFactory.createProxy(GeneGeneticInteractionDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private GeneGeneticInteractionService geneGeneticInteractionService;
	private List<List<Long>> idBatches;

	public GeneGeneticInteractionCurationIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	protected void index() {
		geneGeneticInteractionService = new GeneGeneticInteractionService();
		try {
			log.info("Fetching all GeneGeneticInteraction IDs...");
			SearchResponse<Long> idsResponse = geneGeneticInteractionApi.getAllIds();
			List<Long> allIds = idsResponse.getResults();
			log.info("Fetched {} GeneGeneticInteraction IDs", allIds.size());

			idBatches = partition(allIds, indexerConfig.getBufferSize());
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
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
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

				SearchResponse<GeneGeneticInteraction> response = geneGeneticInteractionApi.findByIds(batchIds);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					continue;
				}

				List<GeneGeneticInteractionDocument> documentsToIndex = new ArrayList<>();
				List<GeneGeneticInteraction> interactions = geneGeneticInteractionService.getFilteredAndReversedInteractions(response.getResults());

				for (GeneGeneticInteraction interaction : interactions) {
					GeneGeneticInteractionDocument document = new GeneGeneticInteractionDocument();
					document.setGeneGeneticInteraction(interaction);
					documentsToIndex.add(document);
				}

				indexDocuments(documentsToIndex, PublicView.GeneticInteraction.class);
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				ExceptionCatcher.report(e);
				System.exit(-1);
				return;
			}
		}
	}
}
