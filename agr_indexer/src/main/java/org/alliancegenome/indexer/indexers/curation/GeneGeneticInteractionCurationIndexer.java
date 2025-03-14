package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.GeneGeneticInteractionDocument;
import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.GeneGeneticInteractionService;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneGeneticInteractionCurationIndexer extends Indexer {


	private GeneGeneticInteractionService geneGeneticInteractionService;

	public GeneGeneticInteractionCurationIndexer(IndexerConfig config) {
		super(config);
	}
	
	@Override
	protected void index() {
		geneGeneticInteractionService = new GeneGeneticInteractionService();
		try {
			SearchResponse<GeneGeneticInteraction> interactionResponse = geneGeneticInteractionService.getGeneGeneticInteractions(0, 0);
			log.info("GeneGeneticInteraction count: " + interactionResponse.getTotalResults());
			
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
				SearchResponse<GeneGeneticInteraction> ggiResponse = geneGeneticInteractionService.getGeneGeneticInteractions(Integer.valueOf(page), indexerConfig.getBufferSize());

				if (ggiResponse == null || CollectionUtils.isEmpty(ggiResponse.getResults())) {
					return;
				}
				
				List<GeneGeneticInteractionDocument> documentsToIndex = new ArrayList<>();
				List<GeneGeneticInteraction> interactions = geneGeneticInteractionService.getFilteredAndReversedInteractions(ggiResponse.getResults());
				
				for (GeneGeneticInteraction interaction : interactions) {
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
	
	private GeneGeneticInteractionDocument createDocument(GeneGeneticInteraction interaction) {
		GeneGeneticInteractionDocument document = new GeneGeneticInteractionDocument();
		document.setGeneGeneticInteraction(interaction);
		return document;
	}
}