package org.alliancegenome.indexer.indexers;

import org.alliancegenome.core.translators.document.FeatureTranslator;
import org.alliancegenome.es.index.site.cache.FeatureDocumentCache;
import org.alliancegenome.es.index.site.document.FeatureDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.Feature;
import org.alliancegenome.neo4j.repository.FeatureIndexerRepository;
import org.alliancegenome.neo4j.repository.FeatureRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

public class FeatureIndexer extends Indexer<FeatureDocument> {

    private final Logger log = LogManager.getLogger(getClass());
    private FeatureDocumentCache featureDocumentCache;
    private FeatureIndexerRepository repo;

    public FeatureIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {
        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            repo = new FeatureIndexerRepository();
            featureDocumentCache = repo.getFeatureDocumentCache(System.getProperty("SPECIES"));

            List<String> fulllist = featureDocumentCache.getFeatureMap().keySet().stream().collect(Collectors.toList());
            queue.addAll(fulllist);

            initiateThreading(queue);
        } catch (InterruptedException e) {
            log.error("Error while indexing...", e);
        }

    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        ArrayList<Feature> list = new ArrayList<>();
        FeatureTranslator featureTranslator = new FeatureTranslator();
        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    saveDocuments(featureTranslator.translateEntities(list));
                    repo.clearCache();
                    list.clear();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        Iterable <FeatureDocument> featureDocuments = featureTranslator.translateEntities(list);
                        featureDocumentCache.addCachedFields(featureDocuments);
                        saveDocuments(featureDocuments);
                        repo.clearCache();
                        list.clear();
                    }
                    return;
                }

                String key = queue.takeFirst();
                Feature feature = featureDocumentCache.getFeatureMap().get(key);
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