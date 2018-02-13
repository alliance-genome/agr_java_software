package org.alliancegenome.indexer.indexers;


import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.shared.es.document.site_index.DiseaseAnnotationDocument;
import org.alliancegenome.shared.neo4j.entity.node.DOTerm;
import org.alliancegenome.shared.neo4j.repository.DiseaseRepository;
import org.alliancegenome.shared.translators.DiseaseTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

public class DiseaseAnnotationIndexer extends Indexer<DiseaseAnnotationDocument> {

    private final Logger log = LogManager.getLogger(getClass());

    public DiseaseAnnotationIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {
        DiseaseRepository diseaseRepository = new DiseaseRepository();

            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            Set<String> allDiseaseIDs = diseaseRepository.getAllDiseaseWithAnnotationsKeys();
            queue.addAll(allDiseaseIDs);
            diseaseRepository.clearCache();
            startSingleThread(queue);


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
                    list = new ArrayList<>();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        saveDocuments(diseaseTrans.translateAnnotationEntities(list, 1));
                        list.clear();
                        repo.clearCache();
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
                return;
            }
        }
    }

}
