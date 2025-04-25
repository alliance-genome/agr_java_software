package org.alliancegenome.indexer.indexers;

import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.curation_api.model.document.es.AffectedGenomicModelDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.curation.service.ModelService;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class AffectedGenomicModelIndexer extends Indexer {

	private final ModelService service = new ModelService();

	public AffectedGenomicModelIndexer(IndexerConfig config) {
		super(config);
	}

	private ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
	private int totalRecords;

	@Override
	public void index() {
		try {
			Set<String> allModelIds = service.getAllModelIds();
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>(allModelIds);
			display.startProcess("Pulling Affective Genomic Model Documents", queue.size());
			totalRecords = allModelIds.size();
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

				String key = queue.takeFirst();
				SearchResponse<AffectedGenomicModelDocument> response = service.getModelDocument(key);
				if (CollectionUtils.isEmpty(response.getResults())) {
					log.debug("No model found for " + key);
					continue;
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
