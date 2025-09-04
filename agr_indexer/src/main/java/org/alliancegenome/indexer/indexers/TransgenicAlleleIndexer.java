package org.alliancegenome.indexer.indexers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.TransgenicAlleleDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.TransgenicAlleleDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.apache.commons.collections.CollectionUtils;
import si.mazi.rescu.RestProxyFactory;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class TransgenicAlleleIndexer extends Indexer {

	private final TransgenicAlleleDocumentInterface transgenicAlleleApi = RestProxyFactory.createProxy(TransgenicAlleleDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private final HashMap<String, Object> params = new HashMap<>() {{
		put("internal", false);
		put("obsolete", false);
		//put("alleleAssociationSubject.primaryExternalId", "WB:WBTransgene00015957");
	}};

	public TransgenicAlleleIndexer(IndexerConfig config) {
		super(config);

	}

	@Override
	protected void index() {
		try {
			SearchResponse<TransgenicAlleleDocument> diseaseSummaryResponse = transgenicAlleleApi.findDocuments(0, 0, params);
			int totalPages = (int) (diseaseSummaryResponse.getTotalResults() / indexerConfig.getBufferSize());
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i <= totalPages; i++) {
				queue.add(String.valueOf(i));
			}
			initiateThreading(queue);
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			System.exit(-1);
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
				SearchResponse<TransgenicAlleleDocument> response = transgenicAlleleApi.findDocuments(Integer.valueOf(page), indexerConfig.getBufferSize(), params);
				if (response == null) {
					return;
				}
				// not every affected genomic model has a document associated to a gene
				if (CollectionUtils.isEmpty(response.getResults())) {
					continue;
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
