package org.alliancegenome.indexer.indexers;

import java.net.InetAddress;
import java.util.Date;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.mapping.Mapping;
import org.apache.log4j.Logger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

public abstract class Indexer extends Thread {

	private Logger log = Logger.getLogger(getClass());
	private IndexerConfig indexConfig;
	private String newIndexName = null;
	private String currentIndex = null;
	private PreBuiltTransportClient client;

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



	//
	//
	//	for(DocumentEntityType det: DocumentEntityType.values()) {
	//
	//		try {
	//			Indexer indexer = (Indexer)det.getIndexerClass().newInstance();
	//			indexer.init();
	//			indexer.start();
	//			indexer.finish();
	//			indexers.add(indexer);
	//		} catch (Exception e) {
	//			e.printStackTrace();
	//		}
	//
	//	}
	//
	//	System.out.println("Waiting for Indexers to finish");
	//	for(Indexer i: indexers) {
	//		try {
	//			i.join();
	//		} catch (InterruptedException e) {
	//			e.printStackTrace();
	//		}
	//	}


	public void createAlias(String alias, String index) {
		log.debug("Creating Alias: " + alias + " for index: " + index);
		// Client Put Alias -> Index
	}
	public void removeAlias(String alias, String index) {
		log.debug("Removing Alias: " + alias + " for index: " + index);
		// Client Remove Alias from Index
	}
	public void createIndex(String index) {
		log.debug("Creating index: " + index);

		try {
			log.debug("Getting Mapping for index: " + indexConfig.getIndexName());
			Mapping mappingClass = (Mapping)indexConfig.getMappingClazz().getDeclaredConstructor(Boolean.class).newInstance(true);
			String mapping = mappingClass.buildMapping();

			log.debug(mapping);
			client.admin().indices().prepareCreate(index).addMapping(indexConfig.getIndexName(), mappingClass.getBuilder()).get();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void deleteIndex(String index) {
		log.debug("Deleting Index: " + index);
		// Client Delete Index
	}

	public void setCurrentIndex() {
		// Get current Index (config.getIndexName()) -- current = self.es.indices.get(self.es_index, ignore=[400, 404])

		// if current == null
		//    current = null
		//    currentName = null
		// else
		//    currentName = index

		// if currentName != Null
		//    get indexs (config.getIndexName())_*
		//    for index in indexes
		//        if index != currentName
		//            delete index(index)

		// return currentName
	}

	private void startIndex() {
		log.debug("Starting " + indexConfig.getIndexName() + ": ");
		newIndexName = indexConfig.getIndexName() + "_" + (new Date()).getTime();
		setCurrentIndex();
		createIndex(newIndexName);
		
		
		client.admin().indices().prepareRefresh(indexConfig.getIndexName()).get();
		
	}
	private void finishIndex() {
		// Refresh Indexes -- self.es.indices.refresh(index=self.new_index_name)
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
