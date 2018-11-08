package org.alliancegenome.indexer.indexers;

import org.alliancegenome.core.translators.document.GoTranslator;
import org.alliancegenome.es.index.site.document.GoDocument;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.repository.GoRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

public class GoIndexer extends Indexer<GoDocument> {

    private final Logger log = LogManager.getLogger(getClass());

    private final GoRepository goRepo = new GoRepository();
    private final GoTranslator goTrans = new GoTranslator();

    private Map<String, Set<String>> geneMap;


    public GoIndexer(IndexerConfig config) {
        super(config);
    }

    @Override
    public void index() {

        LinkedBlockingDeque<String> queue = new LinkedBlockingDeque<>();
        List<String> fulllist = goRepo.getAllGoKeys();
        geneMap = goRepo.getGoTermToGeneMap();

        queue.addAll(fulllist);
        goRepo.clearCache();

        startSingleThread(queue);

    }

    protected void startSingleThread(LinkedBlockingDeque<String> queue) {
        ArrayList<GOTerm> list = new ArrayList<>();
        GoRepository repo = new GoRepository();

        log.info("Pulling All Terms");
        
        Iterable<GOTerm> terms = repo.getAllTerms();
        log.info("Pulling All Terms Finished");
        
        Iterable<GoDocument> docs = goTrans.translateEntities(terms);
        log.info("Translation Done");
        
        saveDocuments(docs);
        log.info("saveDocuments Done");
        
//      while (true) {
//          try {
//              if (list.size() >= indexerConfig.getBufferSize()) {
//                  log.info("saveDocumentsA: " + list.size());
//                  saveDocuments(goTrans.translateEntities(list));
//                  log.info("saveDocumentsB: " + list.size());
//                  //repo.clearCache();
//                  list.clear();
//              }
//              if (queue.isEmpty()) {
//                  if (list.size() > 0) {
//                      log.info("saveDocumentsC: " + list.size());
//                      saveDocuments(goTrans.translateEntities(list));
//                      log.info("saveDocumentsD: " + list.size());
//                      //repo.clearCache();
//                      list.clear();
//                  }
//                  return;
//              }
//
//              String key = queue.takeFirst();
//              GOTerm term = repo.getOneGoTerm(key);
//              if (term != null) {
//                  list.add(term);
//              } else {
//                  log.debug("No go term found for " + key);
//              }
//          } catch (Exception e) {
//              log.error("Error while indexing...", e);
//              System.exit(-1);
//              return;
//          }
//      }
    }

}
