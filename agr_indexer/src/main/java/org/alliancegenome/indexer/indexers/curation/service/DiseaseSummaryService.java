package org.alliancegenome.indexer.indexers.curation.service;

import java.util.HashMap;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.DiseaseSummaryDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.DiseaseSummaryInterface;

import si.mazi.rescu.RestProxyFactory;

public class DiseaseSummaryService {
	private DiseaseSummaryInterface doTermApi = RestProxyFactory.createProxy(DiseaseSummaryInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	
	public SearchResponse<DiseaseSummaryDocument> getDiseaseSummary(Integer page, Integer limit) {
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		return doTermApi.findSummary(page, limit, params);
	}
}
