package org.alliancegenome.indexer.indexers;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.util.StatsCollector;
import org.alliancegenome.es.index.ESDocument;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Indexer extends Thread {

	protected Runtime runtime = Runtime.getRuntime();
	protected DecimalFormat df = new DecimalFormat("#");
	protected ObjectMapper om = new ObjectMapper();

	private ProcessDisplayHelper display = new ProcessDisplayHelper();
	private StatsCollector stats = new StatsCollector();

	protected Map<String,Double> popularityScore;

	private PrintWriter writer = null;
	private Integer threadCount = 1;

	public Indexer(Integer threadCount) {
		this.threadCount = threadCount;
		customizeObjectMapper(om);

		loadPopularityScore();
		try {
			writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream("/data/" + getClass().getSimpleName() + "_data.json")));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadPopularityScore() {

		popularityScore = new HashMap<>();

		try {
			Path popularityFile = Paths.get(ConfigHelper.getPopularityFileName());
			if (!Files.exists(popularityFile)) {
				FileUtils.copyURLToFile(new URL(ConfigHelper.getPopularityDownloadUrl()), popularityFile.toFile());
			}
			popularityScore = Files.lines(popularityFile).collect(Collectors.toMap(key -> String.valueOf(key.split("\t")[0]), val -> Double.valueOf(val.split("\t")[1])));
		} catch (IOException e) {
			e.printStackTrace();
			log.error(e.getMessage());
			System.exit(-1);
		}

	}

	public void runIndex() {
		try {
			display.startProcess(getClass().getSimpleName());
			index();
			display.finishProcess();
			stats.printOutput();
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			System.exit(-1);
		}
	}

	@Override
	public void run() {
		super.run();
		try {
			display.startProcess(getClass().getSimpleName());
			index();
			display.finishProcess();
			stats.printOutput();
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			System.exit(-1);
		}
	}

	public <D extends ESDocument> void saveJsonDocuments(Iterable<D> docs) {
		saveJsonDocuments(docs, null);
	}

	public <D extends ESDocument> void saveJsonDocuments(Iterable<D> docs, Class<?> view) {
		for (D doc : docs) {
			saveJsonDocument(doc, view);
		}
	}
	
	public <D extends ESDocument> void saveJsonDocument(D doc) {
		saveJsonDocument(doc, null);
	}
	
	public <D extends ESDocument> void saveJsonDocument(D doc, Class<?> view) {
		try {
			String json = "";
			if(view != null) {
				json = om.writerWithView(view).writeValueAsString(doc);
			} else {
				json = om.writeValueAsString(doc);
			}
			writer.println(json);
			display.progressProcess();
			stats.addDocument(json);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			log.error(e.getMessage());
			System.exit(-1);
		}
	}
	
	public void initiateThreading(LinkedBlockingDeque<String> queue) throws InterruptedException {

		List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0; i < threadCount; i++) {
			Thread t = new Thread(new Runnable() {
				public void run() {
					startSingleThread(queue);
				}
			});
			threads.add(t);
			t.start();
		}
		
		while (queue.size() > 0) {
			TimeUnit.SECONDS.sleep(10);
		}

		for (Thread t : threads) {
			t.join();
		}
	}

	protected abstract void index();
	protected abstract void startSingleThread(LinkedBlockingDeque<String> queue);
	protected abstract void customizeObjectMapper(ObjectMapper objectMapper);
}
