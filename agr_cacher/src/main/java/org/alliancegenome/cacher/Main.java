package org.alliancegenome.cacher;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.alliancegenome.cacher.cachers.Cacher;
import org.alliancegenome.cacher.config.CacherConfig;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.variant.config.VariantConfigHelper;
import org.alliancegenome.es.util.ProcessDisplayHelper;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {

	public static void main(String[] args) {
		VariantConfigHelper.init();
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
				cachers.put(cc.getCacherName(), i);
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e.getMessage());
				System.exit(-1);
			}
		}
		
		for(Entry<String, Cacher> entry: cachers.entrySet()) {
			String type = entry.getKey();
			if (argumentSet.size() == 0 || argumentSet.contains(type)) {
				if (ConfigHelper.isThreaded()) {
					log.info("Starting in threaded mode for: " + type);
					cachers.get(type).start();
				} else {
					log.info("Starting cacher sequentially: " + type);
					cachers.get(type).run();
				}
			} else {
				log.info("Not Starting: " + type);
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

		Date end = new Date();
		log.info("End Time: " + end);
		long duration = end.getTime() - start.getTime();
		log.info("Total Caching time: " + ProcessDisplayHelper.getHumanReadableTimeDisplay(duration));
		//System.exit(0);

	}
}
