package org.alliancegenome.indexer.indexers;

import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.ESDocument;
import org.alliancegenome.indexer.mapping.Mapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import lombok.ToString;

public abstract class Indexer<D extends ESDocument> extends Thread {

	private Logger log = LogManager.getLogger(getClass());
	protected IndexerConfig indexConfig;
	private String newIndexName = null;
	private String currentIndex = null;
	private PreBuiltTransportClient client;
	protected Runtime runtime = Runtime.getRuntime();
	protected DecimalFormat df = new DecimalFormat("#.00");

	private Date startTime = new Date();
	private Date lastTime = new Date();

	public Indexer(IndexerConfig indexConfig) {
		this.indexConfig = indexConfig;

		client = new PreBuiltTransportClient(Settings.EMPTY);

		try {
			client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ConfigHelper.getEsHost()), ConfigHelper.getEsPort()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	protected abstract void index();

	public void runIndex() {
		startIndex();
		index();
		finishIndex();
	}

	@Override
	public void run() {
		super.run();
		startIndex();
		index();
		finishIndex();
	}


	public void addDocument(D doc) {
		ArrayList<D> docs = new ArrayList<D>();
		docs.add(doc);
		addDocuments(docs);
	}

	public void addDocuments(Iterable<D> docs) {
		log.debug("Adding Documents to ES: ");
		checkMemory();
		log.info(docs.iterator().next());
	}

	protected void startProcess(int amount, int size, int total) {
		log.info("Starting Processing: batches: " + amount + " size: " + size + " total: " + total + " at: " + startTime);
		lastTime = new Date();
	}

	protected void progress(int current, int total, int size) {
		double percent = ((double)current / (double)total);
		Date now = new Date();
		long diff = now.getTime() - startTime.getTime();
		long time = (now.getTime() - lastTime.getTime());
		if(percent > 0) {
			int perms = (int)(diff / percent);
			Date end = new Date(startTime.getTime() + perms);
			log.info("Batch: " + current + " of " + total + " took: " + time + "ms to process " + size + " records at a rate of: " + ((size * 1000) / time) + "r/s, Memory: " + df.format(memoryPercent() * 100) + "%, Percentage complete: " + (int)(percent * 100) + "%, Estimated Finish: " + end);
		} else {
			log.info("Batch: " + current + " of " + total + " took: " + time + "ms to process " + size + " records at a rate of: " + ((size * 1000) / time) + "r/s");
		}
		lastTime = now;
	}

	protected void finishProcess(int total) {
		Date now = new Date();
		long time = now.getTime() - startTime.getTime();
		log.info("Processing finished: took: " + time + "ms to process " + total + " records at a rate of: " + ((total * 1000) / time) + "r/s");
	}

	private void checkMemory() {
		if(memoryPercent() > 0.8) {
			log.info("Memory timeout: " + df.format(memoryPercent() * 100) + "% running blockUntilFinished on current documents");
			log.info("Used Mem: " + (runtime.totalMemory() - runtime.freeMemory()));
			log.info("Free Mem: " + runtime.freeMemory());
			log.info("Total Mem: " + runtime.totalMemory());
			log.info("Max Memory: " + runtime.maxMemory());
		}
	}

	public double memoryPercent() {
		return ((double)runtime.totalMemory() - (double)runtime.freeMemory()) / (double)runtime.maxMemory();
	}

	public void createAlias(String alias, String index) {
		log.debug("Creating Alias: " + alias + " for index: " + index);
		client.admin().indices().prepareAliases().addAlias(index, alias).get();
	}
	public void removeAlias(String alias, String index) {
		log.debug("Removing Alias: " + alias + " for index: " + index);
		client.admin().indices().prepareAliases().removeAlias(index, alias).get();
	}
	public void createIndex(String index) {
		log.debug("Creating index: " + index);

		try {
			log.debug("Getting Mapping for index: " + indexConfig.getIndexName());
			Mapping mappingClass = (Mapping)indexConfig.getMappingClazz().getDeclaredConstructor(Boolean.class).newInstance(true);
			mappingClass.buildMapping();
			//log.debug(mapping);
			client.admin().indices().prepareCreate(index).setSource(mappingClass.getBuilder()).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void deleteIndex(String index) {
		log.debug("Deleting Index: " + index);
		client.admin().indices().prepareDelete(index).get();
	}

	public void setCurrentIndex() {
		try {
			GetIndexResponse t = client.admin().indices().prepareGetIndex().addIndices(indexConfig.getIndexName()).get();
			currentIndex = t.getIndices()[0];
		} catch (Exception e) {
			currentIndex = null;
		}

		if(currentIndex != null) {
			ImmutableOpenMap<String, IndexMetaData> indexList = client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData().getIndices();
			Iterator<String> keys = indexList.keysIt();
			while(keys.hasNext()) {
				String key = keys.next();
				IndexMetaData index = indexList.get(key);

				if(!index.getIndex().getName().equals(currentIndex)) {
					if(index.getIndex().getName().startsWith(indexConfig.getIndexName() + "_")) {
						deleteIndex(index.getIndex().getName());
					}
				}
			}
		}
	}

	private void startIndex() {
		log.debug("Starting " + indexConfig.getIndexName() + ": ");
		newIndexName = indexConfig.getIndexName() + "_" + (new Date()).getTime();
		setCurrentIndex();
		createIndex(newIndexName);
		log.debug("Main Index Starting: ");
	}

	private void finishIndex() {
		log.debug("Main Index Finished: ");
		client.admin().indices().prepareRefresh(newIndexName).get();

		if (currentIndex != null) {
			removeAlias(indexConfig.getIndexName(), currentIndex);
		}

		if (currentIndex != indexConfig.getIndexName()) {
			createAlias(indexConfig.getIndexName(), newIndexName);
			if (currentIndex != null) {
				deleteIndex(currentIndex);
			}
		}

		client.close();
		log.debug(indexConfig.getIndexName() + " Finished: ");
	}


}
