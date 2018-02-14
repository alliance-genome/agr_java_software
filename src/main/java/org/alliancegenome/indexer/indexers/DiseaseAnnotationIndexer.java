package org.alliancegenome.indexer.indexers;


import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.DiseaseAnnotationDocument;
import org.alliancegenome.indexer.entity.node.DOTerm;
import org.alliancegenome.indexer.repository.DiseaseRepository;
import org.alliancegenome.indexer.translators.DiseaseTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class DiseaseAnnotationIndexer extends Indexer<DiseaseAnnotationDocument> {

    private final Logger log = LogManager.getLogger(getClass());

    public DiseaseAnnotationIndexer(String currentIndex, IndexerConfig config) {
        super(currentIndex, config);
    }

    @Override
    public void index() {
        DiseaseRepository diseaseRepository = new DiseaseRepository();
        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            List<String> allDiseaseIDs = diseaseRepository.getAllDiseaseWithAnnotationsKeys();
            queue.addAll(allDiseaseIDs);
            diseaseRepository.clearCache();
            initiateThreading(queue);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        ArrayList<DOTerm> list = new ArrayList<>();
        DiseaseRepository repo = new DiseaseRepository();
        DiseaseTranslator diseaseTrans = new DiseaseTranslator();

        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    addDocuments(diseaseTrans.translateAnnotationEntities(list, 1));
                    repo.clearCache();
                    list.clear();
                    list = new ArrayList<>();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        addDocuments(diseaseTrans.translateAnnotationEntities(list, 1));
                        list.clear();
                        repo.clearCache();
                    }
                    return;
                }

                String key = queue.takeFirst();
                DOTerm disease = repo.getDiseaseTermWithAnnotations(key);
                if (disease != null) {
                    list.add(disease);
                }
            } catch (Exception e) {
                log.error("Error while indexing...", e);
                return;
            }
        }
    }

}
