package org.alliancegenome.indexer.indexers.curation.service;

import java.util.HashMap;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.GeneMolecularInteraction;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneMolecularInteractionInterface;

import si.mazi.rescu.RestProxyFactory;

public class GeneMolecularInteractionService {

	private GeneMolecularInteractionInterface geneMolecularInteractionApi = RestProxyFactory.createProxy(GeneMolecularInteractionInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	
	public SearchResponse<GeneMolecularInteraction> getGeneMolecularInteractions(Integer page, Integer limit) {
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		
		return geneMolecularInteractionApi.find(page, limit, params);
	}

}
