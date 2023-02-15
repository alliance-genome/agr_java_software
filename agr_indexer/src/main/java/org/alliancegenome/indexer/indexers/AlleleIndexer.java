package org.alliancegenome.indexer.indexers;

import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.core.translators.document.AlleleTranslator;
import org.alliancegenome.es.index.site.cache.AlleleDocumentCache;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.indexer.AlleleIndexerRepository;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AlleleIndexer extends Indexer {

	private AlleleDocumentCache alleleDocumentCache;
	private AlleleIndexerRepository repo;
	
	public AlleleIndexer(Integer threadCount) {
		super(threadCount);
	}
	
	@Override
	public void index() {
		try {
			repo = new AlleleIndexerRepository();
			alleleDocumentCache = repo.getAlleleDocumentCache();
			alleleDocumentCache.setPopularity(popularityScore);
			
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>(alleleDocumentCache.getAlleleMap().keySet());

			initiateThreading(queue);
			repo.close();
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			System.exit(-1);
		}

	}

	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		AlleleTranslator alleleTranslator = new AlleleTranslator();
		while (true) {
			try {
				if(queue.isEmpty()) return;
				
				String key = queue.takeFirst();
				Allele allele = alleleDocumentCache.getAlleleMap().get(key);
				if (allele != null) {
					Iterable<AlleleVariantSequence> avsDocs = alleleTranslator.translate(allele);
					alleleDocumentCache.addCachedFields(avsDocs);
					alleleTranslator.updateDocuments(avsDocs);
					saveJsonDocuments(avsDocs, View.AlleleVariantSequenceConverterForES.class);
				} else
					log.debug("No Allele found for " + key);
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				System.exit(-1);
				return;
			}
		}
	}
	
	@Override
	protected void customizeObjectMapper(ObjectMapper objectMapper) {
		objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		objectMapper.setSerializationInclusion(Include.NON_NULL);
	}

}