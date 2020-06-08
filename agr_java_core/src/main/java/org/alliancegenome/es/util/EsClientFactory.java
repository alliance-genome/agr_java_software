package org.alliancegenome.es.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig.Builder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder.RequestConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;

import com.google.common.collect.Multimap;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class EsClientFactory {

    private static RestHighLevelClient client = null;

    public static RestHighLevelClient getDefaultEsClient() {
        if(client == null) {
            client = createClient();
        }
        return client;
    }
    
    public static RestHighLevelClient createNewClient() {
        client = createClient();
        return client;
    }

    // Used if APP needs to have multiple clients
    private static RestHighLevelClient createClient() {

        List<HttpHost> esHosts = new ArrayList<HttpHost>();

        Multimap<String, Integer> map = ConfigHelper.getEsHostMap();

        for(String host: map.keySet()) {
            Collection<Integer> ports = map.get(host);
            for(Integer port: ports) {
                esHosts.add(new HttpHost(host, port));
                log.info("Adding Host: " + host + ":" + port);
            }
        }

        HttpHost[] hosts = new HttpHost[esHosts.size()];
        hosts = esHosts.toArray(hosts);

        client = new RestHighLevelClient(
                RestClient.builder(hosts)
                .setRequestConfigCallback(
                        new RequestConfigCallback() {
                            @Override
                            public Builder customizeRequestConfig(Builder requestConfigBuilder) {
                                return requestConfigBuilder
                                        .setConnectTimeout(5000)
                                        .setSocketTimeout(1800000)
                                        .setConnectionRequestTimeout(1800000)
                                        ;
                            }
                        }
                        )
                );
        log.info("Finished Connecting to ES");
        return client;
    }

}
