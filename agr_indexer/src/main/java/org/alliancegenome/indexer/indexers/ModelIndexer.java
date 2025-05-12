package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.translators.document.ModelTranslator;
import org.alliancegenome.curation_api.model.document.es.AffectedGenomicModelDocument;
import org.alliancegenome.curation_api.model.document.es.DiseaseSummaryDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.index.site.cache.ModelDocumentCache;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneModelInterface;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.alliancegenome.neo4j.repository.indexer.ModelIndexerRepository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class ModelIndexer extends Indexer {

	private ModelDocumentCache cache;
	private ModelIndexerRepository repo;
	private final GeneModelInterface modelApi = RestProxyFactory.createProxy(GeneModelInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private HashMap<String, Object> params = new HashMap<String, Object>() {{
		put("internal", false);
		put("obsolete", false);
	}};

	public ModelIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	protected void index() {
		try {
			SearchResponse<AffectedGenomicModelDocument> diseaseSummaryResponse = modelApi.findForPublic(0, 0, params);
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
				SearchResponse<AffectedGenomicModelDocument> response = modelApi.findForPublic(Integer.valueOf(page), indexerConfig.getBufferSize(), params);
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

}
