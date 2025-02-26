package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.GeneToGeneOrthologyDocument;
import org.alliancegenome.curation_api.model.entities.orthology.GeneToGeneOrthologyGenerated;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.GeneToGeneOrthologyService;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneToGeneOrthologyIndexer extends Indexer {

	private GeneToGeneOrthologyService service = new GeneToGeneOrthologyService();

	public GeneToGeneOrthologyIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	public void index() {
		try {
			log.info("Getting orthologs");
	
			SearchResponse<GeneToGeneOrthologyGenerated> orthologyResponse = service.getGeneToGeneOrthology(0, 0);
	
			log.info("GeneToGeneParalogy count: " + orthologyResponse.getTotalResults());
	
			int totalPages = (int)(orthologyResponse.getTotalResults() / indexerConfig.getBufferSize());
	
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i <= totalPages; i++) {
				queue.add(String.valueOf(i));
			}
			
			initiateThreading(queue);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}

	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		while(true) {
            try {
                if (queue.isEmpty()) {
                    return;
                }
                String page = queue.takeFirst();
                log.info(queue.size() + " pages to process " + Thread.currentThread().getName() + " starting page: " + page);
                SearchResponse<GeneToGeneOrthologyGenerated> resp = service.getGeneToGeneOrthology(Integer.valueOf(page), indexerConfig.getBufferSize());
				List<GeneToGeneOrthologyDocument> docs = createGeneToGeneOrthologyDocuments(resp.getResults());
	
                indexDocuments(docs);
            } catch (Exception e) {
                log.error("Error while indexing...", e);
                System.exit(-1);
                return;
            }
        }
	}

	private List<GeneToGeneOrthologyDocument> createGeneToGeneOrthologyDocuments(List<GeneToGeneOrthologyGenerated> g2gOrthoList) {
		List<GeneToGeneOrthologyDocument> documents = new ArrayList<>();
		for (GeneToGeneOrthologyGenerated g2gOrtho : g2gOrthoList) {
			GeneToGeneOrthologyDocument document1 = new GeneToGeneOrthologyDocument();
			document1.setGeneToGeneOrthologyGenerated(g2gOrtho);

			if (g2gOrtho.getStrictFilter()) {
				document1.setStringencyFilter("stringent");
			} else if (g2gOrtho.getModerateFilter()) {
				document1.setStringencyFilter("moderate");
			}
			
			documents.add(document1);

			//this may cause duplicates with 
			//could create a map with unique pairs
						//gene_id1_gene_id2
						//gene_id2_gene_id1
						//this would be unique
						//create map as I iterate
			//could just remove the flip now if a gene doesn't have it's fliped version
			//may need to make a 
			//just remove this flip for now, identify what genes are not connected to their pair
			// GeneToGeneOrthologyDocument document2 = new GeneToGeneOrthologyDocument();
			// GeneToGeneOrthologyGenerated geneToGeneOrthologyGenerated = new GeneToGeneOrthologyGenerated();
			// geneToGeneOrthologyGenerated.setObjectGene(g2gOrtho.getSubjectGene());
			// geneToGeneOrthologyGenerated.setSubjectGene(g2gOrtho.getObjectGene());
			// geneToGeneOrthologyGenerated.setIsBestScore(g2gOrtho.getIsBestScore());
			// geneToGeneOrthologyGenerated.setIsBestScoreReverse(g2gOrtho.getIsBestScoreReverse());
			// geneToGeneOrthologyGenerated.setConfidence(g2gOrtho.getConfidence());
			// geneToGeneOrthologyGenerated.setStrictFilter(g2gOrtho.getStrictFilter());
			// geneToGeneOrthologyGenerated.setModerateFilter(g2gOrtho.getModerateFilter());
			// geneToGeneOrthologyGenerated.setPredictionMethodsMatched(g2gOrtho.getPredictionMethodsMatched());
			// geneToGeneOrthologyGenerated.setPredictionMethodsNotMatched(g2gOrtho.getPredictionMethodsNotMatched());
			// geneToGeneOrthologyGenerated.setPredictionMethodsNotCalled(g2gOrtho.getPredictionMethodsNotCalled());
			
			// document2.setGeneToGeneOrthologyGenerated(geneToGeneOrthologyGenerated);
			// documents.add(document2);
		}
		return documents;
	}

}
