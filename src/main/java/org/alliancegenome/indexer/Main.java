package org.alliancegenome.indexer;

import java.util.Date;
import java.util.HashMap;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

	private static Logger log = LogManager.getLogger(Main.class);
	
	public static void main(String[] args) {
		ConfigHelper.init();

		HashMap<String, Indexer> indexers = new HashMap<String, Indexer>();

		boolean threaded = ConfigHelper.isThreaded();

		log.info("Start Time: " + new Date());

		for(IndexerConfig ic: IndexerConfig.values()) {
			try {
				Indexer i = (Indexer)ic.getIndexClazz().getDeclaredConstructor(IndexerConfig.class).newInstance(ic);
				indexers.put(ic.getIndexName(), i);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for(int i = 0; i < args.length; i++) {
			log.info("Args[" + i + "]: " + args[i]);
		}
		
		for(String name: indexers.keySet()) {
			if(threaded) {
				log.info("Starting in threaded mode for: " + name);
				indexers.get(name).start();
			} else {
				if(args.length > 0 && args[0].equals(name)) {
					log.info("Starting one indexer: " + name);
					indexers.get(name).runIndex();
				} else if(args.length == 0) {
					log.info("Starting indexer sequentially: " + name);
					indexers.get(name).runIndex();
				} else {
					log.info("Not Starting: " + name);
				}
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

		log.info("End Time: " + new Date());
		System.exit(0);
		
	}
}
