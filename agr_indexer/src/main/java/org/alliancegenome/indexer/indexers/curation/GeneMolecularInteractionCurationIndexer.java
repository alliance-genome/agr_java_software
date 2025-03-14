package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.GeneMolecularInteractionDocument;
import org.alliancegenome.curation_api.model.entities.GeneMolecularInteraction;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.GeneMolecularInteractionService;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneMolecularInteractionCurationIndexer extends Indexer {

	private GeneMolecularInteractionService geneMolecularInteractionService;

	public GeneMolecularInteractionCurationIndexer(IndexerConfig config) {
		super(config);
	}
	
	@Override
	protected void index() {
		geneMolecularInteractionService = new GeneMolecularInteractionService();
		try {
			SearchResponse<GeneMolecularInteraction> interactionResponse = geneMolecularInteractionService.getGeneMolecularInteractions(0, 0);
			log.info("GeneMolecularInteraction count: " + interactionResponse.getTotalResults());
			
			int totalPages = (int) (interactionResponse.getTotalResults() / indexerConfig.getBufferSize());
			
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
				
				String page = queue.takeFirst();
				log.debug(queue.size() + " pages to process " + Thread.currentThread().getName() + " starting page: " + page);
				SearchResponse<GeneMolecularInteraction> gmiResponse = geneMolecularInteractionService.getGeneMolecularInteractions(Integer.valueOf(page), indexerConfig.getBufferSize());

				if (gmiResponse == null || CollectionUtils.isEmpty(gmiResponse.getResults())) {
					return;
				}
				
				List<GeneMolecularInteractionDocument> documentsToIndex = new ArrayList<>();
				List<GeneMolecularInteraction> interactions = geneMolecularInteractionService.getFilteredAndReversedInteractions(gmiResponse.getResults());
				
				for (GeneMolecularInteraction interaction : interactions) {
					documentsToIndex.add(createDocument(interaction));
				}
				
				indexDocuments(documentsToIndex);
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				System.exit(-1);
				return;
			}
		}
	}
	
	private GeneMolecularInteractionDocument createDocument(GeneMolecularInteraction interaction) {
		GeneMolecularInteractionDocument document = new GeneMolecularInteractionDocument();
		document.setGeneMolecularInteraction(interaction);
		return document;
	}
}