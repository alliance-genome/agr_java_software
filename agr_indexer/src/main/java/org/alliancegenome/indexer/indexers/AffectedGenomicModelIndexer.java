package org.alliancegenome.indexer.indexers;

import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.AffectedGenomicModelDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneModelInterface;
import org.alliancegenome.indexer.indexers.curation.service.ModelService;
import si.mazi.rescu.RestProxyFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class AffectedGenomicModelIndexer extends Indexer {

	private final GeneModelInterface modelApi = RestProxyFactory.createProxy(GeneModelInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public AffectedGenomicModelIndexer(IndexerConfig config) {
		super(config);
	}

	private ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
	private HashMap<String, Object> params = new HashMap<>() {{
		put("internal", false);
		put("obsolete", false);
	}};

	@Override
	public void index() {
		try {
			SearchResponse<AffectedGenomicModelDocument> response = modelApi.findForPublic(0, 1, params);
			int totalPages = (int) (response.getTotalResults() / indexerConfig.getBufferSize());
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
			for (int i = 0; i <= totalPages; i++) {
				queue.add(String.valueOf(i));
			}
			display.startProcess("Pulling Affective Genomic Model Documents", queue.size());
			initiateThreading(queue);
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			System.exit(-1);
		}
	}

	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		List<AffectedGenomicModelDocument> list = new ArrayList<>();
		while (true) {
			try {
				if (list.size() >= indexerConfig.getBufferSize()) {
					indexDocuments(list);
					list.clear();
				}
				if (queue.isEmpty()) {
					if (list.size() > 0) {
						indexDocuments(list);
						list.clear();
					}
					return;
				}
				String page = queue.takeFirst();
				SearchResponse<AffectedGenomicModelDocument> response = modelApi.findForPublic(Integer.valueOf(page), indexerConfig.getBufferSize(), params);
				if (response == null || org.apache.commons.collections.CollectionUtils.isEmpty(response.getResults())) {
					return;
				}
				list.addAll(response.getResults());
				display.progressProcess((long) (response.getResults().size()));
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				System.exit(-1);
				return;
			}
		}
	}

}
