package org.alliancegenome.indexer.indexers.curation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.api.entity.GeneSummaryDocument;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.GeneSummaryService;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class GeneSummaryIndexer extends Indexer {

	final private GeneSummaryService service = new GeneSummaryService();

	public GeneSummaryIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	public void index() {
		try {
			log.info("Getting Gene Summary Info");
			SearchResponse<Gene> geneSummaryResponse = service.getGeneSummary(0, 0);
			log.info("GeneSummary count: " + geneSummaryResponse.getTotalResults());
			int totalPages = (int) (geneSummaryResponse.getTotalResults() / indexerConfig.getBufferSize());
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
		while (true) {
			try {
				if (queue.isEmpty()) {
					return;
				}
				String page = queue.takeFirst();
				SearchResponse<Gene> resp = service.getGeneSummary(Integer.valueOf(page), indexerConfig.getBufferSize());
				List<GeneSummaryDocument> docs = createGeneSummaryDocuments(resp.getResults());

				indexDocuments(docs);
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				System.exit(-1);
				return;
			}
		}
	}

	private List<GeneSummaryDocument> createGeneSummaryDocuments(List<Gene> geneInfoList) {
		List<GeneSummaryDocument> documents = new ArrayList<>();
		for (Gene gene : geneInfoList) {
			GeneSummaryDocument document = new GeneSummaryDocument();
			document.setGene(gene);
			documents.add(document);
		}
		return documents;
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

}
