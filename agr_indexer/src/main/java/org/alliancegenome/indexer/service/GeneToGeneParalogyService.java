package org.alliancegenome.indexer.service;

import java.util.HashMap;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.GeneToGeneParalogy;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.core.config.RestConfig;
import org.alliancegenome.indexer.interfaces.GeneToGeneParalogyInterface;

import si.mazi.rescu.RestProxyFactory;

public class GeneToGeneParalogyService {
	private GeneToGeneParalogyInterface paralogyApi = RestProxyFactory.createProxy(GeneToGeneParalogyInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	
	public SearchResponse<GeneToGeneParalogy> getGeneToGeneParalogy(Integer page, Integer limit) {

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		return paralogyApi.findForPublic(page, limit, params);
	}
}
