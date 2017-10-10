package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.DiseaseDocument;
import org.alliancegenome.indexer.entity.node.DOTerm;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.alliancegenome.indexer.translators.DiseaseTranslator;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class DiseaseIndexer extends Indexer<DiseaseDocument> {

    private final Logger log = LogManager.getLogger(getClass());

    private final DiseaseRepository diseaseRepository = new DiseaseRepository();
    private final DiseaseTranslator diseaseTrans = new DiseaseTranslator();

    public DiseaseIndexer(String currentIndex, IndexerConfig config) {
        super(currentIndex, config);
    }

    @Override
    public void index() {

        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            List<String> allDiseaseIDs = diseaseRepository.getAllDiseaseKeys();
            queue.addAll(allDiseaseIDs);
            diseaseRepository.clearCache();
            Integer numberOfThreads = indexerConfig.getThreadCount();
            ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads, getBasicThreadFactory());
            int index = 0;
            Set<Future> futureSet = new HashSet<>(numberOfThreads);
            while (index++ < numberOfThreads) {
                futureSet.add(executor.submit(() -> startThread(queue)));
            }

            int total = queue.size();
            startProcess(total);
            while (isWorkStillPerformed(queue, futureSet)) {
                TimeUnit.SECONDS.sleep(10);
                progress(queue.size(), total);
            }
            if(!queue.isEmpty())
                throw new RuntimeException("There was an error during the multi-threaded indexing. Aborting...");
            finishProcess(total);
            executor.shutdown();

        } catch (InterruptedException e) {
            log.error("Error while indexing...", e);
        }
    }

    private boolean isWorkStillPerformed(LinkedBlockingDeque<String> queue, Set<Future> futureSet) {
        // check if at least one thread is still working, i.e. is not done
        boolean atLeastOneThreadRunning = false;
        for (Future future : futureSet) {
            if (!future.isDone()) {
                atLeastOneThreadRunning = true;
                break;
            }
        }
        return !queue.isEmpty() && atLeastOneThreadRunning;
    }

    private BasicThreadFactory getBasicThreadFactory() {
        // Create a factory that produces daemon threads with a naming pattern and
        // a priority
        return new BasicThreadFactory.Builder()
                .namingPattern("AGR-Indexer-%d")
                .priority(Thread.MAX_PRIORITY)
                .build();
    }

    private void startThread(LinkedBlockingDeque<String> queue) {
        ArrayList<DOTerm> list = new ArrayList<>();
        DiseaseRepository repo = new DiseaseRepository(); // Due to repo not being thread safe
        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    addDocuments(diseaseTrans.translateEntities(list));
                    list.clear();
                    repo.clearCache();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        addDocuments(diseaseTrans.translateEntities(list));
                        list.clear();
                        repo.clearCache();
                    }
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
                return;
            }
        }
    }


}
