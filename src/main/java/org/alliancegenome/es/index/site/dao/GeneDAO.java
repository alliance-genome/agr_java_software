package org.alliancegenome.es.index.site.dao;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.dao.ESDAO;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;

public class GeneDAO extends ESDAO {

	// This class is going to get replaced by a call to NEO
	
	public Map<String, Object> getById(String id) {

		try {
			GetRequest request = new GetRequest();
			request.id(id);
			request.type("gene");
			request.index(ConfigHelper.getEsIndex());
			GetResponse res = searchClient.get(request).get();
			//log.info(res);
			return res.getSource();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		return null;

	}
}
