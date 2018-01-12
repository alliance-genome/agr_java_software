package org.alliancegenome.api.dao;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.alliancegenome.api.config.ConfigHelper;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.jboss.logging.Logger;

public class ESDAO {

    @Inject
    protected ConfigHelper config;

    private final Logger log = Logger.getLogger(getClass());
    protected static PreBuiltTransportClient searchClient = null; // Make sure to only have 1 of these clients to save on resources

    @PostConstruct
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

    public void setConfig(ConfigHelper config) {
        this.config = config;
    }

    @PreDestroy
    public void close() {
        log.info("Closing Down ES Client");
        searchClient.close();
    }

}
