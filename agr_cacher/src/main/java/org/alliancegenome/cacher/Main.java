package org.alliancegenome.cacher;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.alliancegenome.cacher.cachers.Cacher;
import org.alliancegenome.cacher.config.CacherConfig;
import org.alliancegenome.core.config.ConfigHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    
    private static Logger log = LogManager.getLogger(Main.class);
    
    public static void main( String[] args ) {
        ConfigHelper.init();

        Date start = new Date();
        log.info("Start Time: " + start);
        
        
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                log.error("Thread: " + t.getId() + " has uncaught exceptions");
                e.printStackTrace();
                System.exit(-1);
            }
        });
        
        HashMap<String, Cacher> cachers = new HashMap<>();
        for (CacherConfig cc : CacherConfig.values()) {
            try {
                Cacher i = (Cacher) cc.getIndexClazz().getDeclaredConstructor(CacherConfig.class).newInstance(cc);
                cachers.put(cc.getCacheName(), i);
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

        for (String type : cachers.keySet()) {
            if (argumentSet.size() == 0 || argumentSet.contains(type)) {
                if (ConfigHelper.isThreaded()) {
                    log.info("Starting in threaded mode for: " + type);
                    cachers.get(type).start();
                } else {
                    log.info("Starting cacher sequentially: " + type);
                    cachers.get(type).runCache();
                }
            } else {
                log.info("Not Starting: " + type);
            }
        }
        
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


        Date end = new Date();
        log.info("End Time: " + end);
        long duration = end.getTime() - start.getTime();
        log.info("Total Indexing time: " + Cacher.getHumanReadableTimeDisplay(duration));
        System.exit(0);
        
        
    }
}
