package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.shared.es.document.site_index.GoDocument;
import org.alliancegenome.shared.neo4j.entity.node.GOTerm;
import org.alliancegenome.shared.neo4j.repository.GoRepository;
import org.alliancegenome.shared.translators.GoTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class GoIndexer extends Indexer<GoDocument> {

    private final Logger log = LogManager.getLogger(getClass());

    private final GoRepository goRepo = new GoRepository();
    private final GoTranslator goTrans = new GoTranslator();


    public GoIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {
        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            List<String> fulllist = goRepo.getAllGoKeys();
            queue.addAll(fulllist);
            goRepo.clearCache();
            initiateThreading(queue);
        } catch (InterruptedException e) {
            log.error("Error while indexing...", e);
        }
    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        ArrayList<GOTerm> list = new ArrayList<>();
        GoRepository repo = new GoRepository();
        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    saveDocuments(goTrans.translateEntities(list));
                    repo.clearCache();
                    list.clear();
                    list = new ArrayList<>();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        saveDocuments(goTrans.translateEntities(list));
                        repo.clearCache();
                        list.clear();
                    }
                    return;
                }

                String key = queue.takeFirst();
                GOTerm term = repo.getOneGoTerm(key);
                if (term != null) {
                    list.add(term);
                } else {
                    log.debug("No go term found for " + key);
                }
            } catch (Exception e) {
                log.error("Error while indexing...", e);
                System.exit(-1);
                return;
            }
        }
    }

}
