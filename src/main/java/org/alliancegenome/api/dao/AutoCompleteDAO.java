package org.alliancegenome.api.dao;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.alliancegenome.api.config.ConfigHelper;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.jboss.logging.Logger;

@ApplicationScoped
@SuppressWarnings("serial")
public class AutoCompleteDAO {

	private Logger log = Logger.getLogger(getClass());

	private PreBuiltTransportClient searchClient;

	private ConfigHelper config = new ConfigHelper();

	private List<String> response_fields = new ArrayList<String>() {
		{
			add("name"); add("symbol"); add("href"); add("category");
		}
	};

	public AutoCompleteDAO() {
		log.info("AutoComplete Starting: ");
		searchClient = new PreBuiltTransportClient(Settings.EMPTY);
		try {
			searchClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(config.getEsHost()), config.getEsPort()));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}


	public SearchResponse performQuery(QueryBuilder query) {
		SearchRequestBuilder srb = searchClient.prepareSearch();
		srb.setFetchSource(response_fields.toArray(new String[response_fields.size()]), null);
		srb.setIndices(config.getEsIndex());
		srb.setQuery(query);
		log.info("AutoComplete Performing Query: " + srb);
		SearchResponse res = srb.execute().actionGet();
		return res;
	}



}
