package org.alliancegenome.es.index;

import java.io.IOException;

import org.alliancegenome.es.util.EsClientFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.client.RestHighLevelClient;

public class ESDAO {

	private Log log = LogFactory.getLog(getClass());

	protected static RestHighLevelClient searchClient = null; // Make sure to only have 1 of these clients to save on resources

	public ESDAO() {
		init();
	}

	public void init() {
		searchClient = EsClientFactory.getDefaultEsClient();
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
