package org.alliancegenome.indexer;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.shared.config.ConfigHelper;
import org.alliancegenome.shared.es.util.IndexManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Main {

    private static Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {

        Date start = new Date();
        log.info("Start Time: " + start);
        
        IndexManager im = new  IndexManager();
        Indexer.indexName = im.startSiteIndex();

        HashMap<String, Indexer> indexers = new HashMap<>();
        for (IndexerConfig ic : IndexerConfig.values()) {
            try {
                Indexer i = (Indexer) ic.getIndexClazz().getDeclaredConstructor(IndexerConfig.class).newInstance(ic);
                indexers.put(ic.getTypeName(), i);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        Set<String> argumentSet = new HashSet<>();
        for (int i = 0; i < args.length; i++) {
            argumentSet.add(args[i]);
            log.info("Args[" + i + "]: " + args[i]);
        }

        for (String type : indexers.keySet()) {
            if (argumentSet.size() == 0 || argumentSet.contains(type)) {
                if (ConfigHelper.isThreaded()) {
                    log.info("Starting in threaded mode for: " + type);
                    indexers.get(type).start();
                } else {
                    log.info("Starting indexer sequentially: " + type);
                    indexers.get(type).runIndex();
                }
            } else {
                log.info("Not Starting: " + type);
            }
        }

        log.debug("Waiting for Indexers to finish");
        for (Indexer i : indexers.values()) {
            try {
                if (i.isAlive()) {
                    i.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
        
        im.finishIndex();
        
        Date end = new Date();
        log.info("End Time: " + end);
        long duration = end.getTime() - start.getTime();
        log.info("Total Indexing time: " + Indexer.getHumanReadableTimeDisplay(duration));
        System.exit(0);

    }

}
