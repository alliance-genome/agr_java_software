package org.alliancegenome.api.dao;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

@RequestScoped
@SuppressWarnings("serial")
public class AutoCompleteDAO {

	private List<String> response_fields = new ArrayList<String>() {
		{
			add("name"); add("symbol"); add("href"); add("category");
		}
	};
	
	public SearchResponse performQuery(String index, QueryBuilder query) {

		try {
			PreBuiltTransportClient searchClient = new PreBuiltTransportClient(Settings.EMPTY);
			searchClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));

			SearchRequestBuilder srb = searchClient.prepareSearch();

			srb.setFetchSource(response_fields.toArray(new String[response_fields.size()]), null);

			srb.setIndices(index);
			srb.setQuery(query);

			System.out.println(srb);
			SearchResponse res = srb.execute().actionGet();

			searchClient.close();
			return res;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}



}
