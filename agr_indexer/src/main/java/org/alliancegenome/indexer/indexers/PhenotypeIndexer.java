package org.alliancegenome.indexer.indexers;

import org.alliancegenome.core.translators.document.PhenotypeTranslator;
import org.alliancegenome.es.index.site.document.PhenotypeDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.Phenotype;
import org.alliancegenome.neo4j.repository.PhenotypeRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class PhenotypeIndexer extends Indexer<PhenotypeDocument> {

    private final Logger log = LogManager.getLogger(getClass());

    public PhenotypeIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {
        PhenotypeRepository phenotypeRepository = new PhenotypeRepository();
        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            List<String> allDiseaseIDs = phenotypeRepository.getAllPhenotypeKeys();
            queue.addAll(allDiseaseIDs);
            phenotypeRepository.clearCache();
            initiateThreading(queue);
        } catch (InterruptedException e) {
            log.error("Error while indexing...", e);
        }
    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        PhenotypeTranslator phenotypeTranslator = new PhenotypeTranslator();
        List<Phenotype> list = new ArrayList<>();
        PhenotypeRepository repo = new PhenotypeRepository(); // Due to repo not being thread safe
        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    saveDocuments(phenotypeTranslator.translateEntities(list));
                    repo.clearCache();
                    list.clear();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        saveDocuments(phenotypeTranslator.translateEntities(list));
                        repo.clearCache();
                        list.clear();
                    }
                    return;
                }

                String key = queue.takeFirst();
                Phenotype phenotype = repo.getPhenotypeTerm(key);
                if (phenotype != null) {
                    list.add(phenotype);
                } else {
                    log.debug("No phenotype found for " + key);
                }
            } catch (Exception e) {
                log.error("Error while indexing...", e);
                return;
            }
        }
    }


}
