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
import java.util.logging.LogManager;

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

        HashMap<String, Cacher> cachers = new HashMap<>();
        for (CacherConfig cc : CacherConfig.values()) {
            try {
                Cacher i = (Cacher) cc.getCacherClass().getDeclaredConstructor().newInstance();
                cachers.put(cc.getCacheName(), i);
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
                System.exit(-1);
            }
        }

        // Wait for all the DB Cachers to finish
        log.debug("Waiting for Cachers to finish");
        for (Cacher i : cachers.values()) {
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
