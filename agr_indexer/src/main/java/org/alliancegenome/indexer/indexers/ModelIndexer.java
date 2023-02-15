package org.alliancegenome.indexer.indexers;

import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.translators.document.ModelTranslator;
import org.alliancegenome.es.index.site.cache.ModelDocumentCache;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.alliancegenome.neo4j.repository.indexer.ModelIndexerRepository;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModelIndexer extends Indexer {

	private ModelDocumentCache cache;
	private ModelIndexerRepository repo;

	public ModelIndexer(Integer threadCount) {
		super(threadCount);
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
		ModelTranslator translator = new ModelTranslator();

		while (true) {
			try {
				if(queue.isEmpty()) return;
				
				String key = queue.takeFirst();
				AffectedGenomicModel model = cache.getModelMap().get(key);
				if (model != null) {
					SearchableItemDocument document = translator.translate(model);
					cache.addCachedFields(document);
					saveJsonDocument(document);
				} else {
					log.debug("No AffectedGenomicModel found for " + key);
				}
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				System.exit(-1);
				return;
			}
		}
	}
	
	@Override
	protected void customizeObjectMapper(ObjectMapper objectMapper) {
		objectMapper.setSerializationInclusion(Include.NON_NULL);
	}

}
