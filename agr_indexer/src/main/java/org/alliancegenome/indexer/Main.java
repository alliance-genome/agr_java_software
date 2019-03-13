package org.alliancegenome.indexer;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.IndexManager;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        ConfigHelper.init();

        Date start = new Date();
        log.info("Start Time: " + start);

        Boolean keepIndex = ConfigHelper.getKeepIndex();

        IndexManager im = new  IndexManager();

        if (!keepIndex) {
            Indexer.indexName = im.startSiteIndex();
        } else {
            Indexer.indexName = im.getBaseIndexName();
        }

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                log.error("Thread: " + t.getId() + " has uncaught exceptions");
                e.printStackTrace();
                System.exit(-1);
            }
        });

        HashMap<String, Indexer> indexers = new HashMap<>();
        for (IndexerConfig ic : IndexerConfig.values()) {
            try {
                Indexer i = (Indexer) ic.getIndexClazz().getDeclaredConstructor(IndexerConfig.class).newInstance(ic);
                indexers.put(ic.getTypeName(), i);
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
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
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
                System.exit(-1);
            }
        }

        if (!keepIndex) {
            im.finishIndex();
        }

        Date end = new Date();
        log.info("End Time: " + end);
        long duration = end.getTime() - start.getTime();
        log.info("Total Indexing time: " + Indexer.getHumanReadableTimeDisplay(duration));
        System.exit(0);

    }

}
