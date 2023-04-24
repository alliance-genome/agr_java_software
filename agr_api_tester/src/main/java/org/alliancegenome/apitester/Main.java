package org.alliancegenome.apitester;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.alliancegenome.apitester.config.TesterConfig;
import org.alliancegenome.apitester.testers.Tester;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.ProcessDisplayHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
		
		HashMap<String, Tester> testers = new HashMap<>();
		for (TesterConfig tc : TesterConfig.values()) {
			try {
				Tester t = (Tester) tc.getTesterClass().getDeclaredConstructor().newInstance();
				testers.put(tc.getTesterName(), t);
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e.getMessage());
				System.exit(-1);
			}
		}

		// Run all the DB Testers
		for (String type : testers.keySet()) {
			if (argumentSet.size() == 0 || argumentSet.contains(type)) {
				if (ConfigHelper.isThreaded()) {
					log.info("Starting in threaded mode for: " + type);
					testers.get(type).start();
				} else {
					log.info("Starting tester sequentially: " + type);
					testers.get(type).run();
				}
			} else {
				log.info("Not Starting: " + type);
			}
		}

		// Wait for all the DB Testers to finish
		log.debug("Waiting for Testers to finish");
		for (Tester t : testers.values()) {
			try {
				if (t.isAlive()) {
					t.join();
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
		log.info("Total Testing time: " + ProcessDisplayHelper.getHumanReadableTimeDisplay(duration));
		System.exit(0);

	}

}
