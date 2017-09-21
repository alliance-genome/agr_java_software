package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.entity.node.Gene;
import org.alliancegenome.indexer.repository.GeneRepository;
import org.alliancegenome.indexer.translators.GeneTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneIndexer extends Indexer<GeneDocument> {

    private Logger log = LogManager.getLogger(getClass());
    private GeneRepository geneRepo = new GeneRepository();
    private GeneTranslator geneTrans = new GeneTranslator();

    public GeneIndexer(String currnetIndex, IndexerConfig config) {
        super(currnetIndex, config);
    }

    @Override
    public void index() {
        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<String>();
            List<String> fulllist = geneRepo.getAllGeneKeys();
            queue.addAll(fulllist);

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
        ArrayList<Gene> list = new ArrayList<>();
        GeneRepository repo = new GeneRepository();
        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    addDocuments(geneTrans.translateEntities(list));
                    list.clear();
                    list = new ArrayList<>();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        addDocuments(geneTrans.translateEntities(list));
                        list.clear();
                    }
                    return;
                }

                String key = queue.takeFirst();
                Gene gene = repo.getOneGene(key);
                if (gene != null)
                    list.add(gene);
                else
                    log.debug("No disease found for " + key);
            } catch (InterruptedException e) {
                log.error("Error while indexing...", e);
            }
        }
    }

}