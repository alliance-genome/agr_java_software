package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.DiseaseDocument;
import org.alliancegenome.indexer.entity.node.DOTerm;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.alliancegenome.indexer.translators.DiseaseTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class DiseaseIndexer extends Indexer<DiseaseDocument> {

    private Logger log = LogManager.getLogger(getClass());

    private DiseaseRepository diseaseRepository = new DiseaseRepository();
    private DiseaseTranslator diseaseTrans = new DiseaseTranslator();

    public DiseaseIndexer(String currnetIndex, IndexerConfig config) {
        super(currnetIndex, config);
    }

    @Override
    public void index() {


        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            List<String> allDiseaseIDs = diseaseRepository.getAllDiseaseKeys();
            queue.addAll(allDiseaseIDs);

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
        ArrayList<DOTerm> list = new ArrayList<>();
        DiseaseRepository repo = new DiseaseRepository(); // Due to repo not being thread safe
        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    addDocuments(diseaseTrans.translateEntities(list));
                    list.clear();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        addDocuments(diseaseTrans.translateEntities(list));
                        list.clear();
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
            } catch (InterruptedException e) {
                log.error("Error while indexing...", e);
            }
        }
    }


}
