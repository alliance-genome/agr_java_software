package org.alliancegenome.indexer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.IndexManager;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.exceptional.client.ExceptionCatcher;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.indexers.Indexer;
import org.apache.commons.lang3.time.DurationFormatUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

	private Main() {
	}

	public static void main(String[] args) {
		ConfigHelper.init();
		ExceptionCatcher.initialize();

		ProcessDisplayHelper ph = new ProcessDisplayHelper();

		ph.startProcess("Indexer Main: ");

		IndexManager im = new IndexManager();

		Indexer.indexName = im.startSiteIndex();

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			log.error("Thread: " + t.getId() + " has uncaught exceptions");
			e.printStackTrace();
			System.exit(-1);
		});

		HashMap<String, Indexer> indexers = new HashMap<>();
		HashMap<String, Indexer> sequentialMap = new HashMap<>();
		HashMap<String, Indexer> parallelMap = new HashMap<>();

		for (IndexerConfig ic : IndexerConfig.values()) {
			try {
				Indexer i = (Indexer) ic.getIndexClazz().getDeclaredConstructor(IndexerConfig.class).newInstance(ic);
				indexers.put(ic.getTypeName(), i);
				if (ic.getRunInParallel() && ConfigHelper.isThreaded()) {
					parallelMap.put(ic.getTypeName(), i);
				} else {
					sequentialMap.put(ic.getTypeName(), i);
				}
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e.getMessage());
				ExceptionCatcher.report(e);
				System.exit(-1);
			}
		}

		Set<String> argumentSet = new HashSet<>();
		for (int i = 0; i < args.length; i++) {
			argumentSet.add(args[i]);
			log.info("Args[" + i + "]: " + args[i]);
		}

		ExecutorService parallelExecutor = Executors.newFixedThreadPool(10);
		ExecutorService sequentialExecutor = Executors.newFixedThreadPool(1);

		for (String type : parallelMap.keySet()) {
			if (argumentSet.size() == 0 || argumentSet.contains(type)) {
				log.info("Running Parallel for: " + type);
				parallelExecutor.execute(indexers.get(type));
				//indexers.get(type).start();
			} else {
				log.info("Not Starting: " + type);
			}
		}
		
		for (String type : sequentialMap.keySet()) {
			if (argumentSet.size() == 0 || argumentSet.contains(type)) {
				log.info("Running Sequential for Neo4j: " + type);
				sequentialExecutor.execute(indexers.get(type));
				//indexers.get(type).start();
			} else {
				log.info("Not Starting: " + type);
			}
		}

		parallelExecutor.shutdown();
		while (!parallelExecutor.isTerminated()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				ExceptionCatcher.report(e);
				e.printStackTrace();
			}
		}
		log.info("Finished Running Curation Indexers");
		sequentialExecutor.shutdown();
		while (!sequentialExecutor.isTerminated()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				ExceptionCatcher.report(e);
				e.printStackTrace();
			}
		}
		log.info("Finished Running Neo4j Indexers");

		log.debug("Waiting for Indexers to finish");
		for (Indexer i : indexers.values()) {
			try {
				if (i.isAlive()) {
					i.join();
				}
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e.getMessage());
				ExceptionCatcher.report(e);
				System.exit(-1);
			}
		}

		for (Entry<String, Indexer> entry : indexers.entrySet()) {
			String elapsed = DurationFormatUtils.formatDuration(entry.getValue().getDuration().toMillis(), "HH:mm:ss", true);
			log.info("Indexer: " + entry.getKey() + " Elapsed time: " + elapsed);
		}

		im.finishIndex();

		ph.finishProcess();

	}

}
