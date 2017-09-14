package org.alliancegenome.indexer.indexers;

import java.net.InetAddress;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
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

	// Used for showing progress
	private Date startTime = new Date();
	private Date lastTime = new Date();
	private int lastSize;

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

		if(((Collection<D>)docs).size() > 0) {
		
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
				log.error("Has Failures in indexer: " + bulkResponse.buildFailureMessage());
				// process failures by iterating through each bulk response item
			}
		}

	}
	
	// Used to show progess when using a queue
	protected void startProcess(int totalDocAmount) {
		log.info("Starting Processing: queue size: " + totalDocAmount + " at: " + startTime);
		lastTime = new Date();
		lastSize = totalDocAmount;
	}
	
	// Used to show process when using a queue
	protected void progress(int currentSize, int totalDocAmount) {
		double percent = ((double)currentSize / (double)totalDocAmount);
		Date now = new Date();
		long diff = now.getTime() - startTime.getTime();
		long time = (now.getTime() - lastTime.getTime());
		if(time == 0) time = 1000; // Divide by Zero Check
		int processedAmount = (lastSize - currentSize);
		if(percent > 0) {
			int perms = (int)(diff / percent);
			Date end = new Date(startTime.getTime() + perms);
			
			log.info("Size: " + (totalDocAmount - currentSize) + " of " + totalDocAmount + " took: " + time + "ms to process " + processedAmount + " records at a rate of: " + ((processedAmount * 1000) / time) + "r/s, Memory: " + df.format(memoryPercent() * 100) + "%, Percentage complete: " + (int)(percent * 100) + "%, Estimated Finish: " + end);
		} else {
			log.info("Size: " + (totalDocAmount - currentSize) + " of " + totalDocAmount + " took: " + time + "ms to process " + processedAmount + " records at a rate of: " + ((processedAmount * 1000) / time) + "r/s");
		}
		lastSize = currentSize;
		lastTime = now;
	}
	
	// Used to show progress when using batches
	protected void startProcess(int amountBatches, int batchSize, int totalDocAmount) {
		log.info("Starting Processing: batches: " + amountBatches + " size: " + batchSize + " total: " + totalDocAmount + " at: " + startTime);
		lastTime = new Date();
	}

	// Used to show progess when using batches
	protected void progress(int currentBatch, int totalBatches, int processedAmount) {
		double percent = ((double)currentBatch / (double)totalBatches);
		Date now = new Date();
		long diff = now.getTime() - startTime.getTime();
		long time = (now.getTime() - lastTime.getTime());
		if(time == 0) time = 1000; // Divide by Zero Check
		if(percent > 0) {
			int perms = (int)(diff / percent);
			Date end = new Date(startTime.getTime() + perms);
			log.info("Batch: " + currentBatch + " of " + totalBatches + " took: " + time + "ms to process " + processedAmount + " records at a rate of: " + ((processedAmount * 1000) / time) + "r/s, Memory: " + df.format(memoryPercent() * 100) + "%, Percentage complete: " + (int)(percent * 100) + "%, Estimated Finish: " + end);
		} else {
			log.info("Batch: " + currentBatch + " of " + totalBatches + " took: " + time + "ms to process " + processedAmount + " records at a rate of: " + ((processedAmount * 1000) / time) + "r/s");
		}
		lastTime = now;
	}

	protected void finishProcess(int totalDocAmount) {
		Date now = new Date();
		long time = now.getTime() - startTime.getTime();
		log.info("Processing finished: took: " + time + "ms to process " + totalDocAmount + " records at a rate of: " + ((totalDocAmount * 1000) / time) + "r/s");
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
