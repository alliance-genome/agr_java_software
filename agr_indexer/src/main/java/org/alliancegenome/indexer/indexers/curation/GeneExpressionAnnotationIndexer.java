package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.GeneExpressionAnnotationDocument;
import org.alliancegenome.curation_api.model.entities.CrossReference;
import org.alliancegenome.curation_api.model.entities.GeneExpressionAnnotation;
import org.alliancegenome.curation_api.model.entities.GeneExpressionExperiment;
import org.alliancegenome.curation_api.model.ingest.dto.fms.ConsolidatedGeneExpressionFmsDTO;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.services.helpers.annotations.GeneExpressionAnnotationUniqueIdHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.GeneExpressionAnnotationService;
import org.alliancegenome.indexer.indexers.curation.service.GeneExpressionExperimentService;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneExpressionAnnotationIndexer extends Indexer {

	GeneExpressionAnnotationService geneExpressionAnnotationService;
  	GeneExpressionExperimentService geneExpressionExperimentService;
	GeneExpressionAnnotationUniqueIdHelper geneExpressionAnnotationUniqueIdHelper;

	public GeneExpressionAnnotationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		try {
			geneExpressionAnnotationService = new GeneExpressionAnnotationService();
			geneExpressionExperimentService = new GeneExpressionExperimentService();
			geneExpressionAnnotationUniqueIdHelper = new GeneExpressionAnnotationUniqueIdHelper();
			SearchResponse<GeneExpressionAnnotation> response = geneExpressionAnnotationService.getGeneExpressionAnnotations(0, 0);
			log.info("GeneExpressionAnnotation count: " + response.getTotalResults());
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
				SearchResponse<GeneExpressionAnnotation> response = geneExpressionAnnotationService.getGeneExpressionAnnotations(Integer.valueOf(page), indexerConfig.getBufferSize());
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					return;
				}
				List<GeneExpressionAnnotationDocument> documentsToIndex = new ArrayList<>();
				for (GeneExpressionAnnotation gea : response.getResults()) {
					GeneExpressionAnnotationDocument geneExpressionAnnotationDocument = new GeneExpressionAnnotationDocument();
					if (gea.getDataProvider().getAbbreviation().equals("MGI") || gea.getDataProvider().getAbbreviation().equals("WB")) {
						ConsolidatedGeneExpressionFmsDTO geneExpressionFmsDTO = new ConsolidatedGeneExpressionFmsDTO();
						geneExpressionFmsDTO.setGeneId(gea.getExpressionAnnotationSubject().getPrimaryExternalId());
						geneExpressionFmsDTO.setAssay(gea.getExpressionAssayUsed().getCurie());
						String experimentId = geneExpressionAnnotationUniqueIdHelper.generateExperimentId(geneExpressionFmsDTO, gea.getEvidenceItem().getCurie());
						SearchResponse<GeneExpressionExperiment> experimentResponse = geneExpressionExperimentService.getGeneExpressionExperiment(0, 10, experimentId);
						if (!experimentResponse.hasErrors()) {
							if (experimentResponse.getSingleResult() != null) {
								List<CrossReference> crossReferences = experimentResponse.getSingleResult().getCrossReferences();
								if (crossReferences != null) {
									gea.setCrossReferences(new ArrayList<>(crossReferences));
								}
							}
						}
					}
					geneExpressionAnnotationDocument.setGeneExpressionAnnotation(gea);
					documentsToIndex.add(geneExpressionAnnotationDocument);
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
