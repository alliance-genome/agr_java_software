package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.GeneToGeneOrthologyDocument;
import org.alliancegenome.curation_api.model.entities.orthology.GeneToGeneOrthologyGenerated;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.GeneToGeneOrthologyService;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneToGeneOrthologyIndexer extends Indexer {

	private GeneToGeneOrthologyService orthoService = new GeneToGeneOrthologyService();

	public GeneToGeneOrthologyIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	public void index() {

		log.info("Getting orthologs");

		List<GeneToGeneOrthologyGenerated> g2gOrthoList = orthoService.getFiltered();

		log.info("Getting orthologs Finished");

		List<GeneToGeneOrthologyDocument> docs = createGeneToGeneOrthologyDocuments(g2gOrthoList);

		log.info("Translation Done");

		indexDocuments(docs);
		log.info("saveDocuments Done");

	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}

	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		// TODO: look into multithreading this
	}

	private List<GeneToGeneOrthologyDocument> createGeneToGeneOrthologyDocuments(List<GeneToGeneOrthologyGenerated> g2gOrthoList) {
		List<GeneToGeneOrthologyDocument> documents = new ArrayList<>();
		for (GeneToGeneOrthologyGenerated g2gOrtho : g2gOrthoList) {
			GeneToGeneOrthologyDocument document1 = new GeneToGeneOrthologyDocument();
			document1.setGeneToGeneOrthologyGenerated(g2gOrtho);
			documents.add(document1);

			GeneToGeneOrthologyDocument document2 = new GeneToGeneOrthologyDocument();
			GeneToGeneOrthologyGenerated geneToGeneOrthologyGenerated = new GeneToGeneOrthologyGenerated();
			geneToGeneOrthologyGenerated.setObjectGene(g2gOrtho.getSubjectGene());
			geneToGeneOrthologyGenerated.setSubjectGene(g2gOrtho.getObjectGene());
			geneToGeneOrthologyGenerated.setIsBestScore(g2gOrtho.getIsBestScore());
			geneToGeneOrthologyGenerated.setIsBestScoreReverse(g2gOrtho.getIsBestScoreReverse());
			geneToGeneOrthologyGenerated.setConfidence(g2gOrtho.getConfidence());
			geneToGeneOrthologyGenerated.setStrictFilter(g2gOrtho.getStrictFilter());
			geneToGeneOrthologyGenerated.setModerateFilter(g2gOrtho.getModerateFilter());
			geneToGeneOrthologyGenerated.setPredictionMethodsMatched(g2gOrtho.getPredictionMethodsMatched());
			geneToGeneOrthologyGenerated.setPredictionMethodsNotMatched(g2gOrtho.getPredictionMethodsNotMatched());
			geneToGeneOrthologyGenerated.setPredictionMethodsNotCalled(g2gOrtho.getPredictionMethodsNotCalled());
			
			document2.setGeneToGeneOrthologyGenerated(geneToGeneOrthologyGenerated);
			documents.add(document2);
		}
		return documents;
	}

}
