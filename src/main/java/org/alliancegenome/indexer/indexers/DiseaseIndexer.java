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
import java.util.concurrent.LinkedBlockingDeque;

public class DiseaseIndexer extends Indexer<DiseaseDocument> {

    private final Logger log = LogManager.getLogger(getClass());

    public DiseaseIndexer(String currentIndex, IndexerConfig config) {
        super(currentIndex, config);
    }

    @Override
    public void index() {
        DiseaseRepository diseaseRepository = new DiseaseRepository();
        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            List<String> allDiseaseIDs = diseaseRepository.getAllDiseaseKeys();
            queue.addAll(allDiseaseIDs);
            diseaseRepository.clearCache();
            initiateThreading(queue);
        } catch (InterruptedException e) {
            log.error("Error while indexing...", e);
        }
    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        DiseaseTranslator diseaseTrans = new DiseaseTranslator();
        List<DOTerm> list = new ArrayList<>();
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
