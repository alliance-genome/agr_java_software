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
import org.alliancegenome.indexer.indexers.curation.service.helpers.GeneInteractionHelper;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneGeneticInteractionCurationIndexer extends Indexer {

	private GeneGeneticInteractionService geneGeneticInteractionService = new GeneGeneticInteractionService();
	private GeneInteractionHelper interactionHelper = new GeneInteractionHelper();

	public GeneGeneticInteractionCurationIndexer(IndexerConfig config) {
		super(config);
	}
	
	@Override
	protected void index() {

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
				SearchResponse<GeneGeneticInteraction> gmiResponse = geneGeneticInteractionService.getGeneGeneticInteractions(Integer.valueOf(page), indexerConfig.getBufferSize());

				if (gmiResponse == null || CollectionUtils.isEmpty(gmiResponse.getResults())) {
					return;
				}
				
				List<GeneGeneticInteractionDocument> documentsToIndex = new ArrayList<>();
				List<GeneGeneticInteraction> forwardInteractions = gmiResponse.getResults();
				
				for (GeneGeneticInteraction forwardInteraction : forwardInteractions) {
					documentsToIndex.add(createDocument(forwardInteraction));
					GeneGeneticInteraction reverseInteraction = interactionHelper.generateReverseInteraction(forwardInteraction);
					if (reverseInteraction != null) {
						documentsToIndex.add(createDocument(reverseInteraction));
					}
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