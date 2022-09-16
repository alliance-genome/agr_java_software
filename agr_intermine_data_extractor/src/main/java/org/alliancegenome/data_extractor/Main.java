package org.alliancegenome.data_extractor;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.data_extractor.config.ExtractorConfig;
import org.alliancegenome.data_extractor.extractors.DataExtractor;
import org.alliancegenome.es.util.ProcessDisplayHelper;

import lombok.extern.log4j.Log4j2;

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

		HashMap<String, DataExtractor> extractors = new HashMap<>();
		for (ExtractorConfig cc : ExtractorConfig.values()) {
			try {
				DataExtractor ex = (DataExtractor) cc.getCacherClass().getDeclaredConstructor().newInstance();
				extractors.put(cc.getExtractorName(), ex);
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e.getMessage());
				System.exit(-1);
			}
		}

		// Run all the DB Cachers
		for (String type : extractors.keySet()) {
			if (argumentSet.size() == 0 || argumentSet.contains(type)) {
				if (ConfigHelper.isThreaded()) {
					log.info("Starting in threaded mode for: " + type);
					extractors.get(type).start();
				} else {
					log.info("Starting cacher sequentially: " + type);
					extractors.get(type).run();
				}
			} else {
				log.info("Not Starting: " + type);
			}
		}

		// Wait for all the DB Cachers to finish
		log.debug("Waiting for Extractors to finish");
		for (DataExtractor ex : extractors.values()) {
			try {
				if (ex.isAlive()) {
					ex.join();
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
		log.info("Total Extracting time: " + ProcessDisplayHelper.getHumanReadableTimeDisplay(duration));
		System.exit(0);

	}

}
