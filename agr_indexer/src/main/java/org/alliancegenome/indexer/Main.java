package org.alliancegenome.indexer;

import java.util.HashMap;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.IndexManager;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.alliancegenome.indexer.processors.ESDocumentProcessor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

	public static void main(String[] args) {
		ConfigHelper.init();

		ProcessDisplayHelper ph = new ProcessDisplayHelper();
		
		ph.startProcess("Indexer Main: ");

		IndexManager im = new IndexManager();

		ESDocumentProcessor processor = new ESDocumentProcessor(im.startSiteIndex());

//		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
//			log.error("Thread: " + t.getId() + " has uncaught exceptions");
//			e.printStackTrace();
//			System.exit(-1);
//		});
//
		HashMap<String, Indexer> indexers = new HashMap<>();
		for (IndexerConfig ic : IndexerConfig.values()) {
			try {
				Indexer i = (Indexer) ic.getIndexClazz().getDeclaredConstructor(Integer.class).newInstance(ic.getThreadCount());
				indexers.put(ic.getTypeName(), i);
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e.getMessage());
				System.exit(-1);
			}
		}
//
//		Set<String> argumentSet = new HashSet<>();
//		for (int i = 0; i < args.length; i++) {
//			argumentSet.add(args[i]);
//			log.info("Args[" + i + "]: " + args[i]);
//		}
//
//		for (String type : indexers.keySet()) {
//			if (argumentSet.size() == 0 || argumentSet.contains(type)) {
//				if (ConfigHelper.isThreaded()) {
//					log.info("Starting in threaded mode for: " + type);
//					indexers.get(type).start();
//				} else {
//					log.info("Starting indexer sequentially: " + type);
//					indexers.get(type).runIndex();
//				}
//			} else {
//				log.info("Not Starting: " + type);
//			}
//		}
//
//		log.info("Waiting for Indexers to finish");
//		for (Indexer i: indexers.values()) {
//			try {
//				if (i.isAlive()) {
//					i.join();
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//				log.error(e.getMessage());
//				System.exit(-1);
//			}
//		}
		
		processor.processIndexes(indexers);
		processor.close();

		im.finishIndex();

		ph.finishProcess();

	}

}
