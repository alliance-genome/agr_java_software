package org.alliancegenome.indexer.indexers.curation.service;

import lombok.extern.slf4j.Slf4j;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneSummaryInterface;
import si.mazi.rescu.RestProxyFactory;

import java.util.HashMap;

@Slf4j
public class GeneSummaryService {
	private final GeneSummaryInterface geneSummaryApi = RestProxyFactory.createProxy(GeneSummaryInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public SearchResponse<Gene> getGeneSummary(Integer page, Integer limit) {

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		params.put("primaryExternalId", "MGI:109583");

		return geneSummaryApi.findForPublic(page, limit, "GeneView", params);
	}
}
