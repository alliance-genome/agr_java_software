package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.GeneMolecularInteractionDocument;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.GeneMolecularInteractionDocumentInterface;
import org.alliancegenome.curation_api.model.entities.GeneMolecularInteraction;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.GeneMolecularInteractionService;
import org.alliancegenome.neo4j.view.PublicView;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class GeneMolecularInteractionCurationIndexer extends Indexer {

	private final GeneMolecularInteractionDocumentInterface geneMolecularInteractionApi = RestProxyFactory.createProxy(GeneMolecularInteractionDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private GeneMolecularInteractionService geneMolecularInteractionService;
	private List<List<Long>> idBatches;

	public GeneMolecularInteractionCurationIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	protected void index() {
		geneMolecularInteractionService = new GeneMolecularInteractionService();
		try {
			log.info("Fetching all GeneMolecularInteraction IDs...");
			SearchResponse<Long> idsResponse = geneMolecularInteractionApi.getAllIds();
			List<Long> allIds = idsResponse.getResults();
			log.info("Fetched {} GeneMolecularInteraction IDs", allIds.size());

			idBatches = partition(allIds, indexerConfig.getBufferSize());
			log.info("Partitioned into {} batches of up to {}", idBatches.size(), indexerConfig.getBufferSize());

			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i < idBatches.size(); i++) {
				queue.add(String.valueOf(i));
			}

			initiateThreading(queue);
		} catch (Exception e) {
			log.error("Error while indexing...", e);
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

				SearchResponse<GeneMolecularInteraction> response = geneMolecularInteractionApi.findByIds(batchIds);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					continue;
				}

				List<GeneMolecularInteractionDocument> documentsToIndex = new ArrayList<>();
				List<GeneMolecularInteraction> interactions = geneMolecularInteractionService.getFilteredAndReversedInteractions(response.getResults());

				for (GeneMolecularInteraction interaction : interactions) {
					GeneMolecularInteractionDocument document = new GeneMolecularInteractionDocument();
					document.setGeneMolecularInteraction(interaction);
					documentsToIndex.add(document);
				}

				indexDocuments(documentsToIndex, PublicView.MolecularInteraction.class);
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
