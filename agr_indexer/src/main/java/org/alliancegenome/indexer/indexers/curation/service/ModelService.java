package org.alliancegenome.indexer.indexers.curation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.AffectedGenomicModelDocument;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneModelInterface;
import si.mazi.rescu.RestProxyFactory;

import java.util.HashMap;
import java.util.Set;

public class ModelService extends BaseService {

	protected ObjectMapper mapper = RestConfig.config.getJacksonObjectMapperFactory().createObjectMapper();

	private final GeneModelInterface modelApi = RestProxyFactory.createProxy(GeneModelInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public Set<String> getAllGeneIds() {
		return getAllNeoAlleleIDs();
	}

	public Set<String> getAllModelIds() {
		return getAllNeoModelIDs();
	}

	public SearchResponse<AffectedGenomicModelDocument> getModelDocument(String modelId) {
		HashMap<String, Object> params = new HashMap<>();
		params.put("primaryExternalId", modelId);
		return modelApi.findForPublic(0, 1, params);
	}
}
