package org.alliancegenome.indexer.indexers;

import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.ESDocument;
import org.alliancegenome.indexer.schema.Mappings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class Indexer<D extends ESDocument> extends Thread {

	private Logger log = LogManager.getLogger(getClass());
	protected IndexerConfig indexConfig;
	private String newIndexName = null;
	private String oldIndexName = null;
	private PreBuiltTransportClient client;
	protected Runtime runtime = Runtime.getRuntime();
	protected DecimalFormat df = new DecimalFormat("#.00");
	protected ObjectMapper om = new ObjectMapper();
	
	private Date startTime = new Date();
	private Date lastTime = new Date();

	public Indexer(IndexerConfig indexConfig) {
		this.indexConfig = indexConfig;

		try {
			// If you are only one node, you must turn off the sniff feature.
			//Settings s = Settings.builder()
					//.put("client.transport.ignore_cluster_name", true)
					//.put("cluster.name", "docker-cluster")
					//.put("client.transport.sniff", false)
					//.build();
			//client = new TransportClient(s, null);
			client = new PreBuiltTransportClient(Settings.EMPTY);
			client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ConfigHelper.getEsHost()), ConfigHelper.getEsPort()));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
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

		BulkRequestBuilder bulkRequest = client.prepareBulk();
		
		for(D doc: docs) {
			try {
				String json = om.writeValueAsString(doc);
				//log.debug("JSON: " + json);
				bulkRequest.add(client.prepareIndex(newIndexName, indexConfig.getIndexName()).setSource(json).setId(doc.getId()));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
		BulkResponse bulkResponse = bulkRequest.get();
		if (bulkResponse.hasFailures()) {
			log.error("Has Failures in indexer");
		    // process failures by iterating through each bulk response item
		}

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
			org.alliancegenome.indexer.schema.Settings settingClass = (org.alliancegenome.indexer.schema.Settings)indexConfig.getSettingsClazz().getDeclaredConstructor(Boolean.class).newInstance(true);
			Mappings mappingClass = (Mappings)indexConfig.getMappingsClazz().getDeclaredConstructor(Boolean.class).newInstance(true);
			
			mappingClass.buildMappings(true);
			settingClass.buildSettings(true);
			
			//log.debug(settingClass.getBuilder().string());
			//log.debug(mappingClass.getBuilder().string());

			CreateIndexResponse t = client.admin().indices().create(new CreateIndexRequest(index).settings(settingClass.getBuilder().string()).mapping(indexConfig.getIndexName(), mappingClass.getBuilder().string())).get();
			log.debug(t.toString());
		} catch (Exception e) {
			client.admin().indices().prepareRefresh(newIndexName).get();
			log.error("Indexing Failed: " + index);
			e.printStackTrace();
			System.exit(0);
		}
	}
	public void deleteIndex(String index) {
		log.debug("Deleting Index: " + index);
		client.admin().indices().prepareDelete(index).get();
	}

	public void setCurrentIndex() {
		try {
			GetIndexResponse t = client.admin().indices().prepareGetIndex().addIndices(indexConfig.getIndexName()).get();
			//log.debug("Index Found: " + t.getIndices()[0]);
			oldIndexName = t.getIndices()[0];
		} catch (Exception e) {
			oldIndexName = null;
		}
		log.info("Current Index: " + oldIndexName);
		
		if(oldIndexName != null) {
			ImmutableOpenMap<String, IndexMetaData> indexList = client.admin().cluster().prepareState().get().getState().getMetaData().getIndices();
			Iterator<String> keys = indexList.keysIt();
			while(keys.hasNext()) {
				String key = keys.next();
				IndexMetaData index = indexList.get(key);

				if(!index.getIndex().getName().equals(oldIndexName)) {
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

		if (oldIndexName != null) {
			removeAlias(indexConfig.getIndexName(), oldIndexName);
		}

		if (oldIndexName != indexConfig.getIndexName()) {
			createAlias(indexConfig.getIndexName(), newIndexName);
			if (oldIndexName != null) {
				deleteIndex(oldIndexName);
			}
		}

		client.close();
		log.debug(indexConfig.getIndexName() + " Finished: ");
	}


}
