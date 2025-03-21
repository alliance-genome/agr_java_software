package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.curation_api.model.document.es.DiseaseSummaryDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.DiseaseSummaryService;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiseaseSummaryCurationIndexer extends Indexer {

	DiseaseSummaryService service = new DiseaseSummaryService();
	
	public DiseaseSummaryCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		service = new DiseaseSummaryService();
		try {
			SearchResponse<DiseaseSummaryDocument> diseaseSummaryResponse = service.getDiseaseSummary(0, 0);
			int totalPages = (int) (diseaseSummaryResponse.getTotalResults() / indexerConfig.getBufferSize());
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
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		while (true) {
			try {
				if (queue.isEmpty()) {
					return;
				}
				String page = queue.takeFirst();
				SearchResponse<DiseaseSummaryDocument> resp = service.getDiseaseSummary(Integer.valueOf(page), indexerConfig.getBufferSize());
				List<DiseaseSummaryDocument> documents = new ArrayList<>();
				for(DiseaseSummaryDocument diseaseSummary : resp.getResults()) {
					DiseaseSummaryDocument document = diseaseSummary;
					documents.add(document);
				}

				indexDocuments(documents);
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
