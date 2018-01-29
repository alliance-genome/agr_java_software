package org.alliancegenome.shared.es.dao;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import org.alliancegenome.shared.config.ConfigHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

public class ESDAO {

	protected ConfigHelper config = new ConfigHelper();

	private Log log = LogFactory.getLog(getClass());
	
	protected static PreBuiltTransportClient searchClient = null; // Make sure to only have 1 of these clients to save on resources

	public void init() {
		log.info("Creating New ES Client");

		if(searchClient == null) {
			searchClient = new PreBuiltTransportClient(Settings.EMPTY);
			try {
				if(config.getEsHost().contains(",")) {
					String[] hosts = config.getEsHost().split(",");
					for(String host: hosts) {
						searchClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), config.getEsPort()));
					}
				} else {
					searchClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(config.getEsHost()), config.getEsPort()));
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		log.info("Closing Down ES Client");
		searchClient.close();
	}

	// This method is responsible to make sure that the data index is created
	protected void checkIndex(String index) {
		IndicesExistsRequest request = new IndicesExistsRequest(index);
		try {
			IndicesExistsResponse res = searchClient.admin().indices().exists(request).get();
			if(!res.isExists()) {
				log.info(index + " not found creating it");
				Settings settings = Settings.builder()
					.put("index.number_of_replicas", 0)
					.build();
				searchClient.admin().indices().create(new CreateIndexRequest(index).settings(settings)).get();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
}
