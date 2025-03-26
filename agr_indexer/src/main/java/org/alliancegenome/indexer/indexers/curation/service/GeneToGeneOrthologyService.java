package org.alliancegenome.indexer.indexers.curation.service;

import java.util.HashMap;

import org.alliancegenome.api.entity.GeneToGeneOrthologyDocument;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneToGeneOrthologyGeneratedInterface;

import si.mazi.rescu.RestProxyFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneToGeneOrthologyService {
	private final GeneToGeneOrthologyGeneratedInterface orthologyApi = RestProxyFactory.createProxy(GeneToGeneOrthologyGeneratedInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public SearchResponse<GeneToGeneOrthologyDocument> getGeneToGeneOrthology(Integer page, Integer limit) {

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);

		return orthologyApi.findForPublic(page, limit, params);
	}
}
