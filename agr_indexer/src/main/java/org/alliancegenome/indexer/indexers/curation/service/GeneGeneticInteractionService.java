package org.alliancegenome.indexer.indexers.curation.service;

import java.util.HashMap;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.GeneGeneticInteraction;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneGeneticInteractionInterface;

import si.mazi.rescu.RestProxyFactory;

public class GeneGeneticInteractionService {

	private GeneGeneticInteractionInterface geneGeneticInteractionApi = RestProxyFactory.createProxy(GeneGeneticInteractionInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	
	public SearchResponse<GeneGeneticInteraction> getGeneGeneticInteractions(Integer page, Integer limit) {
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		
		return geneGeneticInteractionApi.find(page, limit, params);
	}

}
