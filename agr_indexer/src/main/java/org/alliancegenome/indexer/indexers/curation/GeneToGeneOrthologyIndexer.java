package org.alliancegenome.indexer.indexers.curation;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.GeneToGeneOrthologyDocument;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.interfaces.document.GeneToGeneOrthologyDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.GeneSearchResultDocument;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.GeneToGeneOrthologyService;
import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class GeneToGeneOrthologyIndexer extends Indexer {

	private final GeneToGeneOrthologyDocumentInterface orthologyApi = RestProxyFactory.createProxy(GeneToGeneOrthologyDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public GeneToGeneOrthologyIndexer(IndexerConfig config) {
		super(config);
	}
	
	
	@Override
	public void index() {
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		
		try {
			SearchResponse<GeneSearchResultDocument> resp = orthologyApi.orthologyApi(0, 0, params);
			log.info("GeneToGeneOrthology count: " + resp.getTotalResults());
			int totalPages = (int) (resp.getTotalResults() / indexerConfig.getBufferSize());
			
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
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		// params.put("primaryExternalId", "Xenbase:XB-GENE-17345583"); //
		// params.put("primaryExternalId", "RGD:621017");
		// params.put("primaryExternalId", "WB:WBGene00003883"); // alleles
		// params.put("primaryExternalId", "ZFIN:ZDB-GENE-110114-3");

		while (true) {
			try {
				if (queue.isEmpty()) {
					return;
				}

				String page = queue.takeFirst();
				// log.info(queue.size() + " pages to process " +
				// Thread.currentThread().getName() + " starting page: " + page);

				SearchResponse<GeneSearchResultDocument> response = orthologyApi.findSearchResult(Integer.valueOf(page), indexerConfig.getBufferSize(), params);
				// log.info("Search Response: " + response);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					return;
				}

				indexDocuments(response.getResults());
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
