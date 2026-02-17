package org.alliancegenome.indexer.indexers.curation.service;

import java.util.HashMap;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.GeneExpressionAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.rest.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneExpressionAnnotationInterface;

import si.mazi.rescu.RestProxyFactory;

public class GeneExpressionAnnotationService {

	private GeneExpressionAnnotationInterface geneExpressionAnnotationClient = RestProxyFactory.createProxy(GeneExpressionAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public SearchResponse<GeneExpressionAnnotation> getGeneExpressionAnnotations(Integer page, Integer limit) {
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		return geneExpressionAnnotationClient.findForPublic(page, limit, params);
	}
}
