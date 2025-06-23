package org.alliancegenome.indexer.indexers;

import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.document.ModelDocumentInterface;
import org.alliancegenome.curation_api.model.document.es.AffectedGenomicModelDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.apache.commons.collections.CollectionUtils;
import si.mazi.rescu.RestProxyFactory;

import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class AffectedGenomicModelIndexer extends Indexer {

	private final ModelDocumentInterface modelApi = RestProxyFactory.createProxy(ModelDocumentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private final HashMap<String, Object> params = new HashMap<>() {{
		put("internal", false);
		put("obsolete", false);
		//put("primaryExternalId", "ZFIN:ZDB-FISH-181119-4");
		//put("gene.primaryExternalId", "ZFIN:ZDB-GENE-990415-8");
	}};

	public AffectedGenomicModelIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	protected void index() {
		try {
			SearchResponse<AffectedGenomicModelDocument> diseaseSummaryResponse = modelApi.findDocuments(0, 0, params);
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
				SearchResponse<AffectedGenomicModelDocument> response = modelApi.findDocuments(Integer.valueOf(page), indexerConfig.getBufferSize(), params);
				if (response == null) {
					return;
				}
				// not every affected genomic model has a document associated to a gene
				if (CollectionUtils.isEmpty(response.getResults())) {
					continue;
				}
				response.getResults().stream()
					.filter(affectedGenomicModelDocument -> affectedGenomicModelDocument.getModel().getPrimaryExternalId().equals("ZFIN:ZDB-FISH-150901-10520")
						|| affectedGenomicModelDocument.getModel().getPrimaryExternalId().equals("ZFIN:ZDB-FISH-150901-4211"))
					.forEach(affectedGenomicModelDocument -> log.info("DEBUG_MODE: " + affectedGenomicModelDocument.getModel()));

				indexDocuments(response.getResults());
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				System.exit(-1);
				return;
			}
		}
	}

}
