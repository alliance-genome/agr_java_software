package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.document.GeneToGeneParalogyDocument;
import org.alliancegenome.curation_api.model.entities.GeneToGeneParalogy;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.core.config.RestConfig;
import org.alliancegenome.core.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.service.GeneToGeneParalogyService;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneToGeneParalogyIndexer extends Indexer {

	GeneToGeneParalogyService service;

	public GeneToGeneParalogyIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index(ProcessDisplayHelper display) {
		service = new GeneToGeneParalogyService();
		try {
			SearchResponse<GeneToGeneParalogy> paralogyResponse = service.getGeneToGeneParalogy(0, 0);
			// log.info("GeneToGeneParalogy count: " + paralogyResponse.getTotalResults());
			display.startProcess(paralogyResponse.getTotalResults());
			int totalPages = (int) (paralogyResponse.getTotalResults() / indexerConfig.getBufferSize());
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i <= totalPages; i++) {
				// log.info("page: " + i + " limit: " + indexerConfig.getBufferSize());
				queue.add(String.valueOf(i));
			}

			initiateThreading(queue);
		} catch (InterruptedException e) {
			ExceptionCatcher.report(e);
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
				// log.info(queue.size() + " pages to process " +
				// Thread.currentThread().getName() + " starting page: " + page);
				SearchResponse<GeneToGeneParalogy> resp = service.getGeneToGeneParalogy(Integer.valueOf(page), indexerConfig.getBufferSize());
				indexDocuments(generateDocuments(resp.getResults()));
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				ExceptionCatcher.report(e);
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
