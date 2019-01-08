package org.alliancegenome.indexer.indexers;

import org.alliancegenome.core.translators.document.AlleleTranslator;
import org.alliancegenome.es.index.site.cache.AlleleDocumentCache;
import org.alliancegenome.es.index.site.document.AlleleDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleIndexerRepository;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

public class AlleleIndexer extends Indexer<AlleleDocument> {

    private final Logger log = LogManager.getLogger(getClass());
    private AlleleDocumentCache alleleDocumentCache;
    private AlleleIndexerRepository repo;

    public AlleleIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {
        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            repo = new AlleleIndexerRepository();
            alleleDocumentCache = repo.getAlleleDocumentCache(System.getProperty("SPECIES"));

            List<String> fulllist = alleleDocumentCache.getAlleleMap().keySet().stream().collect(Collectors.toList());
            queue.addAll(fulllist);

            initiateThreading(queue);
        } catch (InterruptedException e) {
            log.error("Error while indexing...", e);
        }

    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        ArrayList<Allele> list = new ArrayList<>();
        AlleleTranslator alleleTranslator = new AlleleTranslator();
        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    saveDocuments(alleleTranslator.translateEntities(list));
                    repo.clearCache();
                    list.clear();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        Iterable <AlleleDocument> alleleDocuments = alleleTranslator.translateEntities(list);
                        alleleDocumentCache.addCachedFields(alleleDocuments);
                        saveDocuments(alleleDocuments);
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