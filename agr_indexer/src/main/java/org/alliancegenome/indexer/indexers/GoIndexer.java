package org.alliancegenome.indexer.indexers;

import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.translators.document.GoTranslator;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.repository.GoRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GoIndexer extends Indexer {

	private final Logger log = LogManager.getLogger(getClass());

	private final GoRepository goRepo = new GoRepository();
	private final GoTranslator goTrans = new GoTranslator();

	public GoIndexer(IndexerConfig config) {
		super(config);
	}

	@Override
	public void index() {

		log.info("Pulling All Terms");

		Iterable<GOTerm> terms = goRepo.getAllTerms();

		log.info("Pulling All Terms Finished");

		Iterable<SearchableItemDocument> docs = goTrans.translateEntities(terms);
		docs.forEach(doc -> doc.setPopularity(popularityScore.get(doc.getPrimaryKey())));

		log.info("Translation Done");

		indexDocuments(docs);
		goRepo.close();
		log.info("saveDocuments Done");

	}

	protected void startSingleThread(LinkedBlockingDeque<String> queue) {
		// No need to multithread this
	}

}
