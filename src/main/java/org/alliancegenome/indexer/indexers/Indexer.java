package org.alliancegenome.indexer.indexers;

import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.ESDocument;
import org.alliancegenome.indexer.schema.Mappings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class Indexer<D extends ESDocument> extends Thread {

	private Logger log = LogManager.getLogger(getClass());
	protected String currentIndex;
	protected TypeConfig typeConfig;
	private PreBuiltXPackTransportClient client;
	protected Runtime runtime = Runtime.getRuntime();
	protected DecimalFormat df = new DecimalFormat("#.00");
	protected ObjectMapper om = new ObjectMapper();

	private Date startTime = new Date();
	private Date lastTime = new Date();

	public Indexer(String currentIndex, TypeConfig typeConfig) {
		this.currentIndex = currentIndex;
		this.typeConfig = typeConfig;
		
		om.setSerializationInclusion(Include.NON_NULL);
		
		try {
			client = new PreBuiltXPackTransportClient(Settings.EMPTY);
			client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ConfigHelper.getEsHost()), ConfigHelper.getEsPort()));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	protected abstract void index();

	public void runIndex() {
		addMapping();
		index();
	}

	private void addMapping() {
		try {
			Mappings mappingClass = (Mappings)typeConfig.getMappingsClazz().getDeclaredConstructor(Boolean.class).newInstance(true);
			mappingClass.buildMappings();
			log.debug("Getting Mapping for type: " + typeConfig.getTypeName());
			client.admin().indices().preparePutMapping(currentIndex).setType(typeConfig.getTypeName()).setSource(mappingClass.getBuilder().string()).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		super.run();
		addMapping();
		index();
	}


	public void addDocument(D doc) {
		ArrayList<D> docs = new ArrayList<D>();
		docs.add(doc);
		addDocuments(docs);
	}

	public void addDocuments(Iterable<D> docs) {
		checkMemory();

		BulkRequestBuilder bulkRequest = client.prepareBulk();

		for(D doc: docs) {
			try {
				String json = om.writeValueAsString(doc);
				//log.debug("JSON: " + json);
				bulkRequest.add(client.prepareIndex(currentIndex, typeConfig.getTypeName()).setSource(json).setId(doc.getDocumentId()));
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

}
