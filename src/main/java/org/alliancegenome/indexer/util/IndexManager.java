package org.alliancegenome.indexer.util;

import java.net.InetAddress;
import java.util.Date;
import java.util.Iterator;

import org.alliancegenome.indexer.config.ConfigHelper;
import org.alliancegenome.indexer.schema.ESSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient;

public class IndexManager {

	private Logger log = LogManager.getLogger(getClass());
	private PreBuiltXPackTransportClient client;
	private String newIndexName;
	private String oldIndexName;
	private String baseIndexName;
	
	public IndexManager() {
		
		baseIndexName = ConfigHelper.getEsIndex();
		
		try {
			client = new PreBuiltXPackTransportClient(Settings.EMPTY);
			client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(ConfigHelper.getEsHost()), ConfigHelper.getEsPort()));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
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
			ESSettings settings = new ESSettings(true);
			settings.buildSettings(true);
			CreateIndexResponse t = client.admin().indices().create(new CreateIndexRequest(index).settings(settings.getBuilder().string())).get();
			log.debug(t.toString());
		} catch (Exception e) {
			client.admin().indices().prepareRefresh(baseIndexName).get();
			log.error("Indexing Failed: " + index);
			e.printStackTrace();
			System.exit(0);
		}
	}
	public void deleteIndex(String index) {
		log.debug("Deleting Index: " + index);
		client.admin().indices().prepareDelete(index).get();
	}
	
	public void startIndex() {

		newIndexName = baseIndexName + "_" + (new Date()).getTime();
		setCurrentIndex();
		createIndex(newIndexName);
		log.debug("Main Index Starting: ");
	}

	public void finishIndex() {
		log.debug("Main Index Finished: ");
		client.admin().indices().prepareRefresh(newIndexName).get();

		if (oldIndexName != null) {
			removeAlias(baseIndexName, oldIndexName);
		}

		if (oldIndexName != baseIndexName) {
			createAlias(baseIndexName, newIndexName);
			if (oldIndexName != null) {
				deleteIndex(oldIndexName);
			}
		}

		client.close();
		log.debug(baseIndexName + " Finished: ");
	}
	
	public void setCurrentIndex() {
		try {
			GetIndexResponse t = client.admin().indices().prepareGetIndex().addIndices(baseIndexName).get();
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
					if(index.getIndex().getName().startsWith(baseIndexName + "_")) {
						deleteIndex(index.getIndex().getName());
					}
				}
			}
		}
	}
	
	public String getNewIndexName() {
		return newIndexName;
	}

}
