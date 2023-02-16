package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.translators.document.DiseaseTranslator;
import org.alliancegenome.es.index.site.cache.DiseaseDocumentCache;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.indexer.DiseaseIndexerRepository;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiseaseIndexer extends Indexer {

	private DiseaseDocumentCache diseaseDocumentCache;
	private DiseaseRepository repo;
	
	public DiseaseIndexer(Integer threadCount) {
		super(threadCount);
	}
	
	@Override
	public void index() {
		try {
			repo = new DiseaseRepository();
			DiseaseIndexerRepository diseaseIndexerRepository = new DiseaseIndexerRepository();
			diseaseDocumentCache = diseaseIndexerRepository.getDiseaseDocumentCache();
			diseaseDocumentCache.setPopularity(popularityScore);
			List<String> allDiseaseIDs = new ArrayList<>(diseaseDocumentCache.getDiseaseMap().keySet());
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>(allDiseaseIDs);
			diseaseIndexerRepository.clearCache();
			initiateThreading(queue);
			repo.close();
			diseaseIndexerRepository.close();
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			System.exit(-1);
		}
	}

	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		DiseaseTranslator diseaseTrans = new DiseaseTranslator();
		
		while (true) {
			try {
				if(queue.isEmpty()) return;
				
				String key = queue.takeFirst();
				DOTerm disease = repo.getDiseaseTerm(key);

				if (disease != null) {
					SearchableItemDocument diseaseDocument = diseaseTrans.translate(disease);
					diseaseDocumentCache.addCachedFields(diseaseDocument);
					saveJsonDocument(diseaseDocument);
				} else {
					log.debug("No disease found for " + key);
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
