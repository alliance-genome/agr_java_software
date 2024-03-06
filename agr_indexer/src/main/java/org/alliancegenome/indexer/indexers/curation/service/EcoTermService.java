package org.alliancegenome.indexer.indexers.curation.service;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;
import org.alliancegenome.curation_api.response.ObjectResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.rest.interfaces.EcoTermRESTInterface;
import si.mazi.rescu.RestProxyFactory;

public class EcoTermService {

	private EcoTermRESTInterface ecoTermApi = RestProxyFactory.createProxy(EcoTermRESTInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	
	public ECOTerm getEcoTerm(String curie) {
		ObjectResponse<ECOTerm> response = ecoTermApi.find(curie);
		return response.getEntity();
	}

}
