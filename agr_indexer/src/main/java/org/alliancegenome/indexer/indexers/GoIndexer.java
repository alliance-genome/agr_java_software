package org.alliancegenome.indexer.indexers;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.translators.document.GoTranslator;
import org.alliancegenome.es.index.site.document.GoDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.repository.GoRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GoIndexer extends Indexer<GoDocument> {

    private final Logger log = LogManager.getLogger(getClass());

    private final GoRepository goRepo = new GoRepository();
    private final GoTranslator goTrans = new GoTranslator();

    public GoIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {

        LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
        List<String> fulllist = goRepo.getAllGoKeys();

        queue.addAll(fulllist);
        goRepo.clearCache();
        startSingleThread(queue);

    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {

        GoRepository repo = new GoRepository();

        log.info("Pulling All Terms");

        Iterable<GOTerm> terms = repo.getAllTerms();

        log.info("Pulling All Terms Finished");

        Iterable<GoDocument> docs = goTrans.translateEntities(terms);
        log.info("Translation Done");

        saveDocuments(docs);
        log.info("saveDocuments Done");

    }

}
