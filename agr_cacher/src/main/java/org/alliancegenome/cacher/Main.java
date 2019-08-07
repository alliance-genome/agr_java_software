package org.alliancegenome.cacher;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.cache.AllianceCacheManager;
import org.alliancegenome.cacher.cachers.Cacher;
import org.alliancegenome.cacher.config.DBCacherConfig;
import org.alliancegenome.core.config.ConfigHelper;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Log4j2
public class Main {

    public static void main(String[] args) {
        ConfigHelper.init();

        Date start = new Date();
        log.info("Start Time: " + start);

        Set<String> argumentSet = new HashSet<>();
        for (int i = 0; i < args.length; i++) {
            argumentSet.add(args[i]);
            log.info("Args[" + i + "]: " + args[i]);
        }

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            log.error("Thread: " + t.getId() + " has uncaught exceptions");
            e.printStackTrace();
            System.exit(-1);
        });

        // Create all the DB Cachers
        HashMap<String, Cacher> dbcachers = new HashMap<>();
        for (DBCacherConfig cc : DBCacherConfig.values()) {
            try {
                Cacher i = (Cacher) cc.getCacherClass().getDeclaredConstructor().newInstance();
                dbcachers.put(cc.getCacherName(), i);
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
                System.exit(-1);
            }
        }

        // Run all the DB Cachers
        for (String type : dbcachers.keySet()) {
            if (argumentSet.size() == 0 || argumentSet.contains(type)) {
                if (ConfigHelper.isThreaded()) {
                    log.info("Starting in threaded mode for: " + type);
                    dbcachers.get(type).start();
                } else {
                    log.info("Starting cacher sequentially: " + type);
                    dbcachers.get(type).runCache();
                }
            } else {
                log.info("Not Starting: " + type);
            }
        }

        // Wait for all the DB Cachers to finish
        log.debug("Waiting for DBCachers to finish");
        for (Cacher i : dbcachers.values()) {
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
        AllianceCacheManager.close();

        Date end = new Date();
        log.info("End Time: " + end);
        long duration = end.getTime() - start.getTime();
        log.info("Total Indexing time: " + Cacher.getHumanReadableTimeDisplay(duration));
        System.exit(0);

    }
}
