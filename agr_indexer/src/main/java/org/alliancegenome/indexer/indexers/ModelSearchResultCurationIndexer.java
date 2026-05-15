package org.alliancegenome.indexer.indexers;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.util.ListUtils;
import org.alliancegenome.curation_api.interfaces.document.ModelDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.ModelSearchResultDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

/**
 * SCRUM-5124 - id-batched indexer that loads ModelSearchResultDocument JSON from the curation API
 * at /api/model/document and writes it into the public site_index_*.
 *
 * Mirrors {@link GOSearchResultCurationIndexer}: first fetches the full ID set via
 * {@link ModelDocumentInterface#getAllIds}, partitions it into batches of bufferSize, and hydrates
 * each batch via {@link ModelDocumentInterface#findSummaryByIds}.
 *
 * Replaces the legacy Neo4j-fed path implemented by
 * {@link org.alliancegenome.indexer.indexers.ModelIndexer}; both can run in parallel during the
 * cut-over until the legacy is retired.
 */
@Slf4j
public class ModelSearchResultCurationIndexer extends Indexer {

	private final ModelDocumentInterface modelApi = RestProxyFactory.createProxy(ModelDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private List<List<Long>> idBatches;

	public ModelSearchResultCurationIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	protected void index(ProcessDisplayHelper display) {
		try {
			log.info("Fetching all model search result IDs...");
			SearchResponse<Long> idsResponse = modelApi.getAllIds();
			List<Long> allIds = idsResponse.getResults();
			log.info("Fetched {} model search result IDs", allIds.size());
			display.startProcess(allIds.size());
			idBatches = ListUtils.partition(allIds, indexerConfig.getBufferSize());
			log.info("Partitioned into {} batches of up to {}", idBatches.size(), indexerConfig.getBufferSize());

			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i < idBatches.size(); i++) {
				queue.add(String.valueOf(i));
			}

			initiateThreading(queue);
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			ExceptionCatcher.report(e);
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
				String batchIndex = queue.takeFirst();
				List<Long> batchIds = idBatches.get(Integer.parseInt(batchIndex));

				SearchResponse<ModelSearchResultDocument> response = modelApi.findSummaryByIds(batchIds);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
					continue;
				}

				indexDocuments(response.getResults());
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