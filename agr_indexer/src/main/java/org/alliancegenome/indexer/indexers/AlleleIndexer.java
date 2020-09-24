package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.translators.document.AlleleTranslator;
import org.alliancegenome.es.index.site.cache.AlleleDocumentCache;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.indexer.AlleleIndexerRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AlleleIndexer extends Indexer<SearchableItemDocument> {

    private final Logger log = LogManager.getLogger(getClass());
    private AlleleDocumentCache alleleDocumentCache;
    private AlleleIndexerRepository repo;

    public AlleleIndexer(IndexerConfig config) {
        super(config);
        species = ConfigHelper.getSpecies();
    }

    @Override
    public void index() {
        try {
            repo = new AlleleIndexerRepository();
            alleleDocumentCache = repo.getAlleleDocumentCache(species);
            alleleDocumentCache.setPopularity(popularityScore);
            List<String> fulllist = new ArrayList<>(alleleDocumentCache.getAlleleMap().keySet());
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>(fulllist);

            initiateThreading(queue);
        } catch (Exception e) {
            log.error("Error while indexing...", e);
            System.exit(-1);
        }

    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        ArrayList<Allele> list = new ArrayList<>();
        AlleleTranslator alleleTranslator = new AlleleTranslator();
        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    Iterable<SearchableItemDocument> alleleDocuments = alleleTranslator.translateEntities(list);
                    alleleDocumentCache.addCachedFields(alleleDocuments);
                    indexDocuments(alleleDocuments);
                    list.clear();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        Iterable <SearchableItemDocument> alleleDocuments = alleleTranslator.translateEntities(list);
                        alleleDocumentCache.addCachedFields(alleleDocuments);
                        indexDocuments(alleleDocuments);
                        repo.clearCache();
                        list.clear();
                    }
                    return;
                }

                String key = queue.takeFirst();
                Allele allele = alleleDocumentCache.getAlleleMap().get(key);
                if (allele != null)
                    list.add(allele);
                else
                    log.debug("No Allele found for " + key);
            } catch (Exception e) {
                log.error("Error while indexing...", e);
                System.exit(-1);
                return;
            }
        }
    }

}