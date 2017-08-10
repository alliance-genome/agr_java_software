package org.alliancegenome.indexer.indexers;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.Document;
import org.alliancegenome.indexer.mapping.Mapping;
import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

public abstract class Indexer<D extends Document> extends Thread {

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

	
	public void addDocument(D doc) {
		ArrayList<D> docs = new ArrayList<D>();
		docs.add(doc);
		addDocuments(docs);
	}
	
	public void addDocuments(Iterable<D> docs) {
		log.debug("Adding Documents to ES: ");
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
