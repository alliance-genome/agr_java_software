package org.alliancegenome.indexer.indexers;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.translators.document.DiseaseTranslator;
import org.alliancegenome.es.index.site.document.DiseaseAnnotationDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiseaseAnnotationIndexer extends Indexer<DiseaseAnnotationDocument> {

    private final Logger log = LogManager.getLogger(getClass());

    public DiseaseAnnotationIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {
        DiseaseRepository diseaseRepository = new DiseaseRepository();
        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            Set<String> allDiseaseIDs = diseaseRepository.getAllDiseaseWithAnnotationsKeys();
            queue.addAll(allDiseaseIDs);
            diseaseRepository.clearCache();
            initiateThreading(queue);
        } catch (InterruptedException e) {
            log.error("Error while indexing...", e);
        }
    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        List<DOTerm> list = new ArrayList<>();
        DiseaseRepository repo = new DiseaseRepository();
        DiseaseTranslator diseaseTrans = new DiseaseTranslator();

        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    saveDocuments(diseaseTrans.translateAnnotationEntities(list, 1));
                    repo.clearCache();
                    list.clear();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        saveDocuments(diseaseTrans.translateAnnotationEntities(list, 1));
                        repo.clearCache();
                        list.clear();
                        
                    }
                    return;
                }

                String key = queue.takeFirst();
                DOTerm disease = repo.getDiseaseTerm(key);
                if (disease != null) {
                    list.add(disease);
                }
            } catch (Exception e) {
                log.error("Error while indexing...", e);
                System.exit(-1);
                return;
            }
        }
    }

}
