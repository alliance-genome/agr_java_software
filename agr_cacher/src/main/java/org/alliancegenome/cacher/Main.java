package org.alliancegenome.cacher;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.alliancegenome.cacher.cachers.Cacher;
import org.alliancegenome.cacher.config.Caches;
import org.alliancegenome.cacher.config.DBCacherConfig;
import org.alliancegenome.cacher.config.IOCacherConfig;
import org.alliancegenome.core.config.ConfigHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    
    private static Logger log = LogManager.getLogger(Main.class);
    
    public static void main( String[] args ) {
        ConfigHelper.init();

        Date start = new Date();
        log.info("Start Time: " + start);
        
        Set<String> argumentSet = new HashSet<>();
        for (int i = 0; i < args.length; i++) {
            argumentSet.add(args[i]);
            log.info("Args[" + i + "]: " + args[i]);
        }
        
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                log.error("Thread: " + t.getId() + " has uncaught exceptions");
                e.printStackTrace();
                System.exit(-1);
            }
        });
        
        HashMap<String, Cacher> dbcachers = new HashMap<>();
        for (DBCacherConfig cc : DBCacherConfig.values()) {
            try {
                Cacher i = (Cacher) cc.getCacherClazz().getDeclaredConstructor(Caches.class).newInstance(cc.getCache());
                dbcachers.put(cc.getCacherName(), i);
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
                System.exit(-1);
            }
        }

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
        
        
        HashMap<String, Cacher> iocachers = new HashMap<>();
        for (IOCacherConfig cc : IOCacherConfig.values()) {
            try {
                Cacher i = (Cacher) cc.getCacherClazz().getDeclaredConstructor(Caches.class, Caches.class).newInstance(cc.getInputCache(), cc.getOutputCache());
                iocachers.put(cc.getCacherName(), i);
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
                System.exit(-1);
            }
        }

        for (String type : iocachers.keySet()) {
            if (argumentSet.size() == 0 || argumentSet.contains(type)) {
                if (ConfigHelper.isThreaded()) {
                    log.info("Starting in threaded mode for: " + type);
                    iocachers.get(type).start();
                } else {
                    log.info("Starting cacher sequentially: " + type);
                    iocachers.get(type).runCache();
                }
            } else {
                log.info("Not Starting: " + type);
            }
        }
        
        log.debug("Waiting for IOCachers to finish");
        for (Cacher i : iocachers.values()) {
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
