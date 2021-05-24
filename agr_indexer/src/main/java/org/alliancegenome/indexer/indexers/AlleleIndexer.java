package org.alliancegenome.indexer.indexers;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.translators.document.AlleleTranslator;
import org.alliancegenome.es.index.site.cache.AlleleDocumentCache;
import org.alliancegenome.es.index.site.document.SearchableItemDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.indexer.AlleleIndexerRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.logging.log4j.*;

import com.fasterxml.jackson.databind.*;

public class AlleleIndexer extends Indexer<SearchableItemDocument> {

    private final Logger log = LogManager.getLogger(getClass());
    private AlleleDocumentCache alleleDocumentCache;
    private AlleleIndexerRepository repo;

    public AlleleIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {
        try {
            repo = new AlleleIndexerRepository();
            alleleDocumentCache = repo.getAlleleDocumentCache();
            alleleDocumentCache.setPopularity(popularityScore);
            
            LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>(alleleDocumentCache.getAlleleMap().keySet());

            initiateThreading(queue);
            repo.close();
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
                    Iterable<AlleleVariantSequence> avsDocs = alleleTranslator.translateEntities(list);
                    alleleDocumentCache.addCachedFields(avsDocs);
                    alleleTranslator.updateDocuments(avsDocs);
                    //indexDocuments(avsDocs, View.AlleleVariantSequenceConverterForES.class);
                    list.clear();
                }
                if (queue.isEmpty()) {
                    if (list.size() > 0) {
                        Iterable <AlleleVariantSequence> avsDocs = alleleTranslator.translateEntities(list);
                        alleleDocumentCache.addCachedFields(avsDocs);
                        alleleTranslator.updateDocuments(avsDocs);
                        //indexDocuments(avsDocs, View.AlleleVariantSequenceConverterForES.class);
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

    @Override
    protected void configureMapper(ObjectMapper mapper) {
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
    }

}