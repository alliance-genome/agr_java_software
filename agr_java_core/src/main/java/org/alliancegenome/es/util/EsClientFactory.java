package org.alliancegenome.es.util;

import java.io.IOException;
import java.util.*;

import org.alliancegenome.core.config.ConfigHelper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.*;

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

    public static void closeClient() {
        log.info("Closing ES Client: ");
        try {
            client.close();
            client = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}