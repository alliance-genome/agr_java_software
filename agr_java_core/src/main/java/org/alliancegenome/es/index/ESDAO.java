package org.alliancegenome.es.index;

import org.alliancegenome.core.config.ConfigHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ESDAO {

    private Log log = LogFactory.getLog(getClass());

    protected static RestHighLevelClient searchClient = null; // Make sure to only have 1 of these clients to save on resources

    public ESDAO() {
        init();
    }

    public void init() {
        if(searchClient == null) {
            if(ConfigHelper.getEsHost().contains(",")) {
                String[] hostnames = ConfigHelper.getEsHost().split(",");
                List<HttpHost> hosts = Arrays.stream(hostnames).map(host -> new HttpHost(host, ConfigHelper.getEsPort())).collect(Collectors.toList());
                searchClient = new RestHighLevelClient(
                        RestClient.builder((HttpHost[])hosts.toArray())
                );
            } else {
                searchClient = new RestHighLevelClient(RestClient.builder(new HttpHost(ConfigHelper.getEsHost(),ConfigHelper.getEsPort())));
            }
        }
    }

    public void close() {
        log.info("Closing Down ES Client");
        try {
            searchClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
