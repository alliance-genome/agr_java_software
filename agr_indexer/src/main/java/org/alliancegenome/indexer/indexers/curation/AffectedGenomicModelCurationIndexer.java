package org.alliancegenome.indexer.indexers.curation;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.ModelDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.AffectedGenomicModelDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class AffectedGenomicModelCurationIndexer extends Indexer {

	private final ModelDocumentInterface modelApi = RestProxyFactory.createProxy(ModelDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private List<List<Long>> idBatches;

	public AffectedGenomicModelCurationIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	protected void index() {
		try {
			log.info("Fetching all AGM IDs...");
			SearchResponse<Long> idsResponse = modelApi.getAllIds();
			List<Long> allIds = idsResponse.getResults();
			log.info("Fetched {} AGM IDs", allIds.size());

			idBatches = partition(allIds, indexerConfig.getBufferSize());
			log.info("Partitioned into {} batches of up to {}", idBatches.size(), indexerConfig.getBufferSize());

			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i < idBatches.size(); i++) {
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
				String batchIndex = queue.takeFirst();
				List<Long> batchIds = idBatches.get(Integer.parseInt(batchIndex));

				SearchResponse<AffectedGenomicModelDocument> response = modelApi.findByIds(batchIds);
				if (response == null || CollectionUtils.isEmpty(response.getResults())) {
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
