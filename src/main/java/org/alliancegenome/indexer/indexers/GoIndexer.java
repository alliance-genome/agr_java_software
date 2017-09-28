package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.GoDocument;
import org.alliancegenome.indexer.entity.node.GOTerm;
import org.alliancegenome.indexer.repository.GoRepository;
import org.alliancegenome.indexer.translators.GoTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GoIndexer extends Indexer<GoDocument> {

    private final Logger log = LogManager.getLogger(getClass());

    private final GoRepository goRepo = new GoRepository();
    private final GoTranslator goTrans = new GoTranslator();


    public GoIndexer(String currnetIndex, IndexerConfig config) {
        super(currnetIndex, config);
    }

    @Override
    public void index() {
        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<String>();
            List<String> fulllist = goRepo.getAllGoKeys();
            queue.addAll(fulllist);
            goRepo.clearCache();
            Integer numberOfThreads = indexerConfig.getThreadCount();
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
            int index = 0;
            while (index++ < numberOfThreads) {
                executor.submit(() -> startThread(queue));
            }

            int total = queue.size();
            startProcess(total);
            while (!queue.isEmpty()) {
                TimeUnit.SECONDS.sleep(30);
                progress(queue.size(), total);
            }
            finishProcess(total);
            executor.shutdown();

        } catch (InterruptedException e) {
            log.error("Error while indexing...", e);
        }

    }

    private void startThread(LinkedBlockingDeque<String> queue) {
        ArrayList<GOTerm> list = new ArrayList<>();
        GoRepository repo = new GoRepository();
        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    addDocuments(goTrans.translateEntities(list));
                    repo.clearCache();
                    list.clear();
                    list = new ArrayList<>();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        addDocuments(goTrans.translateEntities(list));
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
            } catch (InterruptedException e) {
                log.error("Error while indexing...", e);
            }
        }
    }

}
