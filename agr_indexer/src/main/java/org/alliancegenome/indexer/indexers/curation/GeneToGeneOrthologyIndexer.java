package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.GeneToGeneOrthologyDocument;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.orthology.GeneToGeneOrthologyGenerated;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.GeneToGeneOrthologyService;
import org.apache.commons.collections4.CollectionUtils;

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
			GeneToGeneOrthologyDocument document = new GeneToGeneOrthologyDocument();

			document.setGeneToGeneOrthologyGenerated(g2gOrtho);
			createStringencyFilter(g2gOrtho, document);
			createGeneAnnotations(g2gOrtho, document);
			removeAnnotationLists(document);

			documents.add(document);
		}
		return documents;
	}

	
	private void createStringencyFilter(GeneToGeneOrthologyGenerated g2gOrtho, GeneToGeneOrthologyDocument document) {
		if (Boolean.TRUE.equals(g2gOrtho.getStrictFilter())) {
			document.setStringencyFilter("stringent");
		} else if (Boolean.TRUE.equals(g2gOrtho.getModerateFilter())) {
			document.setStringencyFilter("moderate");
		}
	}

	private void createGeneAnnotations(GeneToGeneOrthologyGenerated g2gOrtho, GeneToGeneOrthologyDocument document) {
		Map<String, Object> map = new HashMap<>();
		putGeneInfo(map, g2gOrtho.getSubjectGene());
		putGeneInfo(map, g2gOrtho.getObjectGene());
		document.setGeneAnnotations(map);
	}
	
	private void putGeneInfo(Map<String, Object> map, Gene gene) {
		Map<String, Object> data = new HashMap<>();
		data.put("hasExpressionAnnotations", hasExpressionAnnotations(gene));
		data.put("hasDiseaseAnnotations", hasDiseaseAnnotations(gene));
		map.put(gene.getIdentifier(), data);
	}

	private boolean hasDiseaseAnnotations(Gene gene) {
		return CollectionUtils.isNotEmpty(gene.getGeneDiseaseAnnotations());
	}

	private boolean hasExpressionAnnotations(Gene gene) {
		return CollectionUtils.isNotEmpty(gene.getGeneExpressionAnnotations());
	}

	private void removeAnnotationLists(GeneToGeneOrthologyDocument document) {
		document.getGeneToGeneOrthologyGenerated().getSubjectGene().setGeneDiseaseAnnotations(null);
		document.getGeneToGeneOrthologyGenerated().getSubjectGene().setGeneExpressionAnnotations(null);
		document.getGeneToGeneOrthologyGenerated().getObjectGene().setGeneDiseaseAnnotations(null);
		document.getGeneToGeneOrthologyGenerated().getObjectGene().setGeneExpressionAnnotations(null);
	}



}
