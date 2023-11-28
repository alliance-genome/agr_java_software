package org.alliancegenome.indexer.indexers.curation.service;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.crud.OrganizationCrudInterface;
import org.alliancegenome.curation_api.model.entities.Organization;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.OrganizationInterface;
import org.apache.commons.collections.CollectionUtils;
import si.mazi.rescu.RestProxyFactory;

import java.util.HashMap;
import java.util.List;

public class OrganizationService {

	private OrganizationInterface organizationApi = RestProxyFactory.createProxy(OrganizationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

/*
	public List<Vocabulary> getAllVocabularies() {
		SearchResponse<Vocabulary> response = vocabularyApi.find(0, 1000, new HashMap<>());
		return response.getResults();
	}
*/

	public Organization getOrganization(String abbreviation) {
		HashMap<String, Object> params = new HashMap<>();
		params.put("abbreviation", abbreviation);
		SearchResponse<Organization> response = organizationApi.find(0, 1000, params);
		List<Organization> list = response.getResults();
		if (CollectionUtils.isEmpty(list) || list.size() > 1) {
			throw new RuntimeException("Could not find organization by abbreviation " + abbreviation);
		}
		return list.get(0);
	}

}
