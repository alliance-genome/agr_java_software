package org.alliancegenome.indexer.service;

import java.util.HashMap;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.orthology.GeneToGeneOrthologyGenerated;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.indexer.interfaces.GeneToGeneOrthologyGeneratedInterface;

import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.RestProxyFactory;

@Slf4j
public class GeneToGeneOrthologyService {
	private final GeneToGeneOrthologyGeneratedInterface orthologyApi = RestProxyFactory.createProxy(GeneToGeneOrthologyGeneratedInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public SearchResponse<GeneToGeneOrthologyGenerated> getGeneToGeneOrthology(Integer page, Integer limit) {

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);

		return orthologyApi.findForPublic(page, limit, params);
	}
}
