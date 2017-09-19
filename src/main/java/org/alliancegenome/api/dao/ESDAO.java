package org.alliancegenome.api.dao;

import org.alliancegenome.api.config.ConfigHelper;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.jboss.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class ESDAO {

	@Inject
	protected ConfigHelper config;

	private Logger log = Logger.getLogger(getClass());
	protected PreBuiltTransportClient searchClient;

	@PostConstruct
	public void init() {
		log.info("Creating New ES Client");

		searchClient = new PreBuiltTransportClient(Settings.EMPTY);
		try {
			searchClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(config.getEsHost()), config.getEsPort()));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void setConfig(ConfigHelper config) {
		this.config = config;
	}

	@PreDestroy
	public void close() {
		log.info("Closing Down ES Client");
		searchClient.close();
	}
	
	public Map<String, Object> getById(String id) {

		try {
			GetRequest request = new GetRequest();
			request.id(id);
			request.index(config.getEsIndex());
			GetResponse res = searchClient.get(request).get();
			//log.info(res);
			return res.getSource();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
}
