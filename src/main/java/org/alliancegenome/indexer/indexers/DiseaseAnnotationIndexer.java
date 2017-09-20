package org.alliancegenome.indexer.indexers;


import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.DiseaseAnnotationDocument;
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

public class DiseaseAnnotationIndexer extends Indexer<DiseaseAnnotationDocument> {

    private Logger log = LogManager.getLogger(getClass());

    private DiseaseRepository diseaseRepository = new DiseaseRepository();
    private DiseaseTranslator diseaseTrans = new DiseaseTranslator();

    public DiseaseAnnotationIndexer(String currentIndex, TypeConfig config) {
        super(currentIndex, config);
    }

    @Override
    public void index() {

        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            List<String> allDiseaseIDs = diseaseRepository.getAllDiseaseKeys();
            queue.addAll(allDiseaseIDs);

            Integer numberOfThreads = typeConfig.getThreadCount();
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
            e.printStackTrace();
        }

    }

    private void startThread(LinkedBlockingDeque<String> queue) {

        ArrayList<DOTerm> list = new ArrayList<DOTerm>();
        while (true) {
            try {
                if (list.size() >= typeConfig.getBufferSize()) {
                    addDocuments(diseaseTrans.translateAnnotationEntities(list, 1));
                    list.clear();
                    list = new ArrayList<>();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        addDocuments(diseaseTrans.translateAnnotationEntities(list, 1));
                        list.clear();
                    }
                    return;
                }

                String key = queue.takeFirst();
                DOTerm disease = diseaseRepository.getDiseaseTermWithAnnotations(key);
                if (disease != null) {
                    list.add(disease);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
