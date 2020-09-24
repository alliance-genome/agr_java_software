package org.alliancegenome.indexer.indexers;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.translators.document.HTPDatasetTranslator;
import org.alliancegenome.es.index.site.cache.DatasetDocumentCache;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.HTPDataset;
import org.alliancegenome.neo4j.repository.indexer.DatasetIndexerRepository;
import org.apache.logging.log4j.*;

public class DatasetIndexer extends Indexer<SearchableItemDocument> {

    private final Logger log = LogManager.getLogger(getClass());
    private DatasetDocumentCache cache;
    private DatasetIndexerRepository repo;

    public DatasetIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {
        try {
            repo = new DatasetIndexerRepository();
            cache = repo.getCache();
            List<String> fulllist = new ArrayList<>(cache.getDatasetMap().keySet());
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>(fulllist);

            initiateThreading(queue);
        } catch (Exception e) {
            log.error("Error while indexing...", e);
            System.exit(-1);
        }

    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        ArrayList<HTPDataset> list = new ArrayList<>();
        HTPDatasetTranslator translator = new HTPDatasetTranslator();
        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    Iterable<SearchableItemDocument> documents = translator.translateEntities(list);
                    cache.addCachedFields(documents);
                    indexDocuments(documents);
                    list.clear();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        Iterable <SearchableItemDocument> documents = translator.translateEntities(list);
                        cache.addCachedFields(documents);
                        indexDocuments(documents);
                        repo.clearCache();
                        list.clear();
                    }
                    return;
                }

                String key = queue.takeFirst();
                HTPDataset entity = cache.getDatasetMap().get(key);
                if (entity != null)
                    list.add(entity);
                else
                    log.debug("No Dataset found for " + key);
            } catch (Exception e) {
                log.error("Error while indexing...", e);
                System.exit(-1);
                return;
            }
        }
    }

}
