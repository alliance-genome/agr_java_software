package org.alliancegenome.indexer.indexers.curation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.AlleleDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.indexers.curation.service.BaseService;
import org.apache.commons.collections.CollectionUtils;
import si.mazi.rescu.RestProxyFactory;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class AlleleSummaryCurationIndexer extends Indexer {

	private final AlleleDocumentInterface alleleApi = RestProxyFactory.createProxy(AlleleDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final BaseService baseService = new BaseService();
	private Set<String> allNeoAlleleIDs = baseService.getAllNeoAlleleIDs();
	private HashMap<String, Object> params = new HashMap<>() {{
		put("internal", false);
		put("obsolete", false);
	}};

	public AlleleSummaryCurationIndexer(IndexerConfig indexerConfig) {
		super(indexerConfig);
	}

	@Override
	protected void index() {
		long cursor = 0;
		while (true) {
			try {
				SearchResponse<AlleleSummaryDocument> response = alleleApi.findSummaryWithCursor(0, indexerConfig.getBufferSize(), cursor, params);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					return;
				}
				cursor = response.getNextCursor();
				indexDocuments(response.getResults());
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				System.exit(-1);
				return;
			}
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {

	}

	@Override
	protected ObjectMapper customizeObjectMapper(ObjectMapper objectMapper) {
		return RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();
	}

}
