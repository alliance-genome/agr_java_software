package org.alliancegenome.indexer.indexers;

import org.alliancegenome.core.translators.document.GeneTranslator;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.es.index.site.document.GeneDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneIndexerRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class GeneIndexer extends Indexer<GeneDocument> {

    private final Logger log = LogManager.getLogger(getClass());
    private GeneDocumentCache geneDocumentCache;

    public GeneIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {

        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            GeneRepository geneRepo = new GeneRepository();
            GeneIndexerRepository geneIndexerRepository = new GeneIndexerRepository();

            List<String> fulllist;
            if (System.getProperty("SPECIES") != null) {
                geneDocumentCache = geneIndexerRepository.getGeneDocumentCache(System.getProperty("SPECIES"));
                fulllist = geneRepo.getAllGeneKeys(System.getProperty("SPECIES"));
            } else {
                geneDocumentCache = geneIndexerRepository.getGeneDocumentCache();
                fulllist = geneRepo.getAllGeneKeys();
            }

            queue.addAll(fulllist);
            geneRepo.clearCache();
            initiateThreading(queue);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        ArrayList<Gene> list = new ArrayList<>();
        GeneTranslator geneTrans = new GeneTranslator();
        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    saveDocuments(geneTrans.translateEntities(list));
                    list.clear();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        Iterable<GeneDocument> geneDocuments = geneTrans.translateEntities(list);
                        geneDocumentCache.addCachedFields(geneDocuments);
                        saveDocuments(geneDocuments);
                        list.clear();
                    }
                    return;
                }

                String key = queue.takeFirst();
                Gene gene = geneDocumentCache.getGenes().get(key);

                if (gene != null)
                    list.add(gene);
                else
                    log.debug("No gene found for " + key);
            } catch (Exception e) {
                log.error("Error while indexing...", e);
                System.exit(-1);
                return;
            }
        }
    }

}
