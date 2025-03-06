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
		}
		return documents;
	}

}
