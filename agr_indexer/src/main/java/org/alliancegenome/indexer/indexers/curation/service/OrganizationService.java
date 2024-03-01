package org.alliancegenome.indexer.indexers.curation.service;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.Organization;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.OrganizationInterface;
import org.apache.commons.collections.CollectionUtils;
import si.mazi.rescu.RestProxyFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrganizationService {

	private final OrganizationInterface organizationApi = RestProxyFactory.createProxy(OrganizationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	Map<String, Organization> orgCacheMap = new HashMap<>();

	public Organization getOrganization(String abbreviation) {
		Organization org = orgCacheMap.get(abbreviation);
		if (org != null) {
			return org;
		}
		HashMap<String, Object> params = new HashMap<>();
		params.put("abbreviation", abbreviation);
		SearchResponse<Organization> response = organizationApi.find(0, 1000, params);
		List<Organization> list = response.getResults();
		if (CollectionUtils.isEmpty(list) || list.size() > 1) {
			throw new RuntimeException("Could not find organization by abbreviation " + abbreviation);
		}
		orgCacheMap.put(abbreviation, list.get(0));
		return list.get(0);
	}

}
