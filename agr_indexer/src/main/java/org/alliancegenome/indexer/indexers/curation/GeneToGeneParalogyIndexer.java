package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.GeneToGeneParalogyDocument;
import org.alliancegenome.curation_api.model.entities.GeneToGeneParalogy;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.GeneToGeneParalogyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneToGeneParalogyIndexer extends Indexer {

	GeneToGeneParalogyService service = new GeneToGeneParalogyService();

	public GeneToGeneParalogyIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		try {
			SearchResponse<GeneToGeneParalogy> paralogyResponse = service.getGeneToGeneParalogy(0, 0);
			//log.info("GeneToGeneParalogy count: " + paralogyResponse.getTotalResults());

			int totalPages = (int) (paralogyResponse.getTotalResults() / indexerConfig.getBufferSize());
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i <= totalPages; i++) {
				//log.info("page: " + i + " limit: " + indexerConfig.getBufferSize());
				queue.add(String.valueOf(i));
			}
			
			initiateThreading(queue);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected List<GeneToGeneParalogyDocument> generateDocuments(List<GeneToGeneParalogy> paralogyList) {
		List<GeneToGeneParalogyDocument> documentList = new ArrayList<>();
		for (GeneToGeneParalogy paralogy : paralogyList) {
			GeneToGeneParalogyDocument document = new GeneToGeneParalogyDocument();
			document.setGeneToGeneParalogy(paralogy);
			documentList.add(document);
		}
		return documentList;
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {

		while (true) {
			try {
				if (queue.isEmpty()) {
					return;
				}
				String page = queue.takeFirst();
				log.info(queue.size() + " pages to process " + Thread.currentThread().getName() + " starting page: " + page);
				SearchResponse<GeneToGeneParalogy> resp = service.getGeneToGeneParalogy(Integer.valueOf(page), indexerConfig.getBufferSize());
				indexDocuments(generateDocuments(resp.getResults()));
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
