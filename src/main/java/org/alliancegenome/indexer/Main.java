package org.alliancegenome.indexer;

import java.util.Date;
import java.util.HashMap;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.util.IndexManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        ConfigHelper.init();

        IndexManager im = new IndexManager();
        HashMap<String, Indexer> indexers = new HashMap<>();

        Date start = new Date();
        log.info("Start Time: " + start);

        im.startIndex();

        for(IndexerConfig ic: IndexerConfig.values()) {
            try {
                Indexer i = (Indexer)ic.getIndexClazz().getDeclaredConstructor(String.class, IndexerConfig.class).newInstance(im.getNewIndexName(), ic);
                indexers.put(ic.getTypeName(), i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        HashMap<String, String> argMap = new HashMap<>();
        for(int i = 0; i < args.length; i++) {
            argMap.put(args[i], args[i]);
            log.info("Args[" + i + "]: " + args[i]);
        }

        for(String type: indexers.keySet()) {
            if(argMap.size() == 0 || argMap.containsKey(type)) {
                if(ConfigHelper.isThreaded()) {
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
        for(Indexer i: indexers.values()) {
            try {
                if(i.isAlive()) {
                    i.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        im.finishIndex();
        Date end = new Date();
        log.info("End Time: " + end);
        log.info("Total Indexing time: " + (int)((end.getTime() - start.getTime()) / 1000) + " seconds");
        System.exit(0);

    }
}
