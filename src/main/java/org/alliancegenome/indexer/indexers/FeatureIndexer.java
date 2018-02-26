package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.shared.es.document.site_index.FeatureDocument;
import org.alliancegenome.shared.neo4j.entity.node.Feature;
import org.alliancegenome.shared.neo4j.repository.FeatureRepository;
import org.alliancegenome.shared.translators.FeatureTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class FeatureIndexer extends Indexer<FeatureDocument> {

    private final Logger log = LogManager.getLogger(getClass());
    private final FeatureRepository featureRepository = new FeatureRepository();

    public FeatureIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {
        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            List<String> fulllist = featureRepository.getAllGeneKeys();
            queue.addAll(fulllist);
            featureRepository.clearCache();
            initiateThreading(queue);
        } catch (InterruptedException e) {
            log.error("Error while indexing...", e);
        }

    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        ArrayList<Feature> list = new ArrayList<>();
        FeatureRepository repo = new FeatureRepository();
        FeatureTranslator geneTrans = new FeatureTranslator();
        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    saveDocuments(geneTrans.translateEntities(list));
                    list.clear();
                    repo.clearCache();
                    list = new ArrayList<>();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        saveDocuments(geneTrans.translateEntities(list));
                        repo.clearCache();
                        list.clear();
                    }
                    return;
                }

                String key = queue.takeFirst();
                Feature feature = repo.getFeature(key);
                if (feature != null)
                    list.add(feature);
                else
                    log.debug("No Feature found for " + key);
            } catch (Exception e) {
                log.error("Error while indexing...", e);
                System.exit(-1);
                return;
            }
        }
    }

}