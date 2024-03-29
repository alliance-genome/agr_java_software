package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.translators.document.ModelTranslator;
import org.alliancegenome.es.index.site.cache.ModelDocumentCache;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.alliancegenome.neo4j.repository.indexer.ModelIndexerRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModelIndexer extends Indexer {

	private ModelDocumentCache cache;
	private ModelIndexerRepository repo;

	public ModelIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	protected void index() {
		try {
			repo = new ModelIndexerRepository();
			cache = repo.getModelDocumentCache();

			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>(cache.getModelMap().keySet());

			initiateThreading(queue);
			repo.close();
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			System.exit(-1);
		}
	}

	@Override
	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		ArrayList<AffectedGenomicModel> list = new ArrayList<>();
		ModelTranslator translator = new ModelTranslator();


		while (true) {
			try {
				if (list.size() >= indexerConfig.getBufferSize()) {
					Iterable <SearchableItemDocument> documents = translator.translateEntities(list);
					cache.addCachedFields(documents);
					indexDocuments(documents);
					list.clear();
				}
				if (queue.isEmpty()) {
					if (list.size() > 0) {
						Iterable <SearchableItemDocument> documents = translator.translateEntities(list);
						cache.addCachedFields(documents);
						indexDocuments(documents);
						repo.clearCache();
						list.clear();
					}
					return;
				}

				String key = queue.takeFirst();
				AffectedGenomicModel model = cache.getModelMap().get(key);
				if (model != null)
					list.add(model);
				else
					log.debug("No AffectedGenomicModel found for " + key);
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				System.exit(-1);
				return;
			}
		}
	}

}
