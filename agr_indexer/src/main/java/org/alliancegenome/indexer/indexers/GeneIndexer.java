package org.alliancegenome.indexer.indexers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.translators.document.GeneTranslator;
import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.es.index.site.document.GeneDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneIndexerRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneIndexer extends Indexer<GeneDocument> {

    private final Logger log = LogManager.getLogger(getClass());
    private GeneDocumentCache geneDocumentCache;
    private String species = null;

    public GeneIndexer(IndexerConfig config) {
        super(config);
        species = ConfigHelper.getSpecies();
    }

    @Override
    public void index() {

        try {
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
            GeneRepository geneRepo = new GeneRepository();
            GeneIndexerRepository geneIndexerRepository = new GeneIndexerRepository();

            List<String> fulllist;
            if (species != null) {
                geneDocumentCache = geneIndexerRepository.getGeneDocumentCache(species);
                fulllist = geneDocumentCache.getGeneMap().keySet().stream().collect(Collectors.toList());
            } else {
                geneDocumentCache = geneIndexerRepository.getGeneDocumentCache();
                fulllist = geneDocumentCache.getGeneMap().keySet().stream().collect(Collectors.toList());
            }

            queue.addAll(fulllist);
            geneRepo.clearCache();
            initiateThreading(queue);
        } catch (Exception e) {
            log.error("Error while indexing...", e);
            System.exit(-1);
        }
    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        ArrayList<Gene> list = new ArrayList<>();
        GeneTranslator geneTrans = new GeneTranslator();
        while (true) {
            try {
                if (list.size() >= indexerConfig.getBufferSize()) {
                    Iterable<GeneDocument> geneDocuments = geneTrans.translateEntities(list);
                    geneDocumentCache.addCachedFields(geneDocuments);
                    saveDocuments(geneDocuments);
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
                Gene gene = geneDocumentCache.getGeneMap().get(key);

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
