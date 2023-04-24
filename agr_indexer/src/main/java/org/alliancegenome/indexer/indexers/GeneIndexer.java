package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.alliancegenome.core.translators.document.GeneTranslator;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.indexer.GeneIndexerRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneIndexer extends Indexer {

	private GeneDocumentCache geneDocumentCache;

	public GeneIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	public void index() {

		try {
			LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();

			GeneIndexerRepository geneIndexerRepository = new GeneIndexerRepository();

			geneDocumentCache = geneIndexerRepository.getGeneDocumentCache();
			List<String> fulllist = geneDocumentCache.getGeneMap().keySet().stream().collect(Collectors.toList());

			geneDocumentCache.setPopularity(popularityScore);

			queue.addAll(fulllist);

			initiateThreading(queue);
			geneIndexerRepository.close();
		} catch (Exception e) {
			log.error("Error while indexing...", e);
			System.exit(-1);
		}
	}

	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		ArrayList<Gene> list = new ArrayList<>();
		GeneTranslator geneTrans = new GeneTranslator();
		while (true) {
			try {
				if (list.size() >= indexerConfig.getBufferSize()) {
					Iterable<SearchableItemDocument> geneDocuments = geneTrans.translateEntities(list);
					geneDocumentCache.addCachedFields(geneDocuments);
					indexDocuments(geneDocuments);
					list.clear();
				}
				if (queue.isEmpty()) {
					if (list.size() > 0) {
						Iterable<SearchableItemDocument> geneDocuments = geneTrans.translateEntities(list);
						geneDocumentCache.addCachedFields(geneDocuments);
						indexDocuments(geneDocuments);
						list.clear();
					}
					return;
				}

				String key = queue.takeFirst();
				Gene gene = geneDocumentCache.getGeneMap().get(key);

				if (gene != null)
					list.add(gene);
				else
					log.debug("No gene found for " + key);
			} catch (Exception e) {
				log.error("Error while indexing...", e);
				System.exit(-1);
				return;
			}
		}
	}
}
