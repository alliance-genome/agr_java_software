package org.alliancegenome.es.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import com.google.common.collect.Multimap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EsClientFactory {

	private static Date lastClientChange = new Date();
	private static RestHighLevelClient client = null;
	public static RequestOptions LARGE_RESPONSE_REQUEST_OPTIONS;

	static {
		RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
		//builder.setHttpAsyncResponseConsumerFactory(new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(1 * 1024 * 1024 * 1024));
		LARGE_RESPONSE_REQUEST_OPTIONS = builder.build();
	}

	public static RestHighLevelClient getDefaultEsClient() {
		Date current = new Date();
		if(client == null) {
			client = createClient();
		} else if(current.getTime() - lastClientChange.getTime() > 180000) {
			RestHighLevelClient currentClient = client;
			client = createClient();
			if(currentClient != null) {
				try {
					currentClient.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		lastClientChange = current;
		return client;
	}
	
	public static RestHighLevelClient getMustCloseSearchClient() {
		return createClient();
	}

	// Used if APP needs to have multiple clients
	private static RestHighLevelClient createClient() {

		List<HttpHost> esHosts = new ArrayList<>();

		Multimap<String, Integer> map = ConfigHelper.getEsHostMap();

		for(String host: map.keySet()) {
			Collection<Integer> ports = map.get(host);
			for(Integer port: ports) {
				esHosts.add(new HttpHost(host, port));
				log.debug("Adding Host: " + host + ":" + port);
			}
		}

		HttpHost[] hosts = new HttpHost[esHosts.size()];
		hosts = esHosts.toArray(hosts);

		log.info("Creating new ES Client: " + map);
		int hours = 2 * (60 * 60 * 1000);
		client = new RestHighLevelClient(
				RestClient.builder(hosts)
						.setRequestConfigCallback(
								// Timeout after 60 * 60 * 1000 milliseconds = 1 hour
								// Needed for long running snapshots
								requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(5000).setSocketTimeout(hours).setConnectionRequestTimeout(hours)
						)
		);

		log.info("Finished Connecting to ES");
		return client;
	}


}