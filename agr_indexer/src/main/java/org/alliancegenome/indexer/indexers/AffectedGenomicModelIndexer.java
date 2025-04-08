package org.alliancegenome.indexer.indexers;

import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.core.translators.document.DiseaseTranslator;
import org.alliancegenome.es.index.site.cache.DiseaseDocumentCache;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.curation.service.ModelService;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.indexer.DiseaseIndexerRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class AffectedGenomicModelIndexer extends Indexer {

	private DiseaseDocumentCache diseaseDocumentCache;

	private ModelService service = new ModelService();
	public AffectedGenomicModelIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	public void index() {
		try {
			Set<String> allGeneIds = service.getAllGeneIds();
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>(allGeneIds);
			initiateThreading(queue);
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			System.exit(-1);
		}
	}

	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		DiseaseTranslator diseaseTrans = new DiseaseTranslator();
		List<DOTerm> list = new ArrayList<>();
		DiseaseRepository repo = new DiseaseRepository(); // Due to repo not being thread safe
		while (true) {
			try {
				if (list.size() >= indexerConfig.getBufferSize()) {

					Iterable<SearchableItemDocument> diseaseDocuments = diseaseTrans.translateEntities(list);
					diseaseDocumentCache.addCachedFields(diseaseDocuments);
					indexDocuments(diseaseDocuments);
					repo.clearCache();
					list.clear();
				}
				if (queue.isEmpty()) {
					if (list.size() > 0) {
						Iterable<SearchableItemDocument> diseaseDocuments = diseaseTrans.translateEntities(list);
						diseaseDocumentCache.addCachedFields(diseaseDocuments);
						indexDocuments(diseaseDocuments);
						repo.clearCache();
						list.clear();
					}
					repo.close();
					return;
				}

				String key = queue.takeFirst();
				DOTerm disease = repo.getDiseaseTerm(key);
				if (disease != null) {
					list.add(disease);
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

}
