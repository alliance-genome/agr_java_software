package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.translators.document.VariantTranslator;
import org.alliancegenome.es.index.site.cache.IndexerCache;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.repository.indexer.VariantIndexerRepository;
import org.apache.logging.log4j.*;

public class VariantIndexer extends Indexer<SearchableItemDocument> {

    private final Logger log = LogManager.getLogger(getClass());
    private VariantIndexerRepository repo;
    private IndexerCache cache;

    public VariantIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    protected void index() {

        //for now, variants only get indexed if an additional flag is set
        if (!ConfigHelper.getIndexVariants()) {
            log.info("Not indexing Variants, use -DINDEX_VARAINTS=\"true\" to include variants");
            return;
        }

        try {
            repo = new VariantIndexerRepository();
            cache = repo.getVariantCache();

            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>(cache.getVariantMap().keySet());

            initiateThreading(queue);
            repo.close();
        } catch (Exception e) {
            log.error("Error while indexing...", e);
            System.exit(-1);
        }
    }

    @Override
    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        ArrayList<Variant> list = new ArrayList<>();
        VariantTranslator translator = new VariantTranslator();

        while(true) {
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
                Variant variant = cache.getVariantMap().get(key);
                if (variant != null)
                    list.add(variant);
                else
                    log.debug("No Variant found for " + key);
            } catch (Exception e) {
                log.error("Error while indexing...", e);
                System.exit(-1);
                return;
            }
        }

    }

}
