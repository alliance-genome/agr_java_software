package org.alliancegenome.indexer.indexers;

import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.translators.document.HTPDatasetTranslator;
import org.alliancegenome.es.index.site.cache.DatasetDocumentCache;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.HTPDataset;
import org.alliancegenome.neo4j.repository.indexer.DatasetIndexerRepository;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DatasetIndexer extends Indexer {

	private DatasetDocumentCache cache;
	private DatasetIndexerRepository repo;

	public DatasetIndexer(Integer threadCount) {
		super(threadCount);
	}
	
	@Override
	public void index() {
		try {
			repo = new DatasetIndexerRepository();
			cache = repo.getCache();

			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>(cache.getDatasetMap().keySet());

			initiateThreading(queue);
			repo.close();
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			System.exit(-1);
		}

	}

	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		HTPDatasetTranslator translator = new HTPDatasetTranslator();
		while (true) {
			try {
				
				String key = queue.takeFirst();
				HTPDataset entity = cache.getDatasetMap().get(key);

				if (entity != null) {
					SearchableItemDocument document = translator.translate(entity);
					cache.addCachedFields(document);
					saveJsonDocument(document);
				} else {
					log.debug("No Dataset found for " + key);
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
