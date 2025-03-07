package org.alliancegenome.indexer.indexers.curation.service;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.GeneExpressionExperiment;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneExpressionExperimentInterface;
import si.mazi.rescu.RestProxyFactory;

import java.util.HashMap;

public class GeneExpressionExperimentService {
	private GeneExpressionExperimentInterface geneExpressionExperimentClient = RestProxyFactory.createProxy(GeneExpressionExperimentInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public SearchResponse<GeneExpressionExperiment> getGeneExpressionExperiments(Integer page, Integer limit) {
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		return geneExpressionExperimentClient.findForPublic(page, limit, params);
	}
}
