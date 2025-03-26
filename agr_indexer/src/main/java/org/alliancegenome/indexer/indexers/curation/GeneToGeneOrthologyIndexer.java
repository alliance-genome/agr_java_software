package org.alliancegenome.indexer.indexers.curation;

import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.GeneToGeneOrthologyDocument;
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

			SearchResponse<GeneToGeneOrthologyDocument> orthologyResponse = service.getGeneToGeneOrthology(0, 0);

			log.info("GeneToGeneOrthology count: " + orthologyResponse.getTotalResults());

			int totalPages = (int) (orthologyResponse.getTotalResults() / indexerConfig.getBufferSize());

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

				SearchResponse<GeneToGeneOrthologyDocument> resp = service.getGeneToGeneOrthology(Integer.valueOf(page), indexerConfig.getBufferSize());

				if (resp == null || CollectionUtils.isEmpty(resp.getResults())) {
					return;
				}

				indexDocuments(resp.getResults());
				
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
