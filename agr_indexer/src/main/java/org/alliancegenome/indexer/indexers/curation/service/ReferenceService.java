package org.alliancegenome.indexer.indexers.curation.service;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.alliancegenome.curation_api.response.ObjectResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.ReferenceInterface;
import si.mazi.rescu.RestProxyFactory;

import java.util.HashMap;
import java.util.Map;

public class ReferenceService {

	private final ReferenceInterface referenceApi = RestProxyFactory.createProxy(ReferenceInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	Map<String, Reference> refCacheMap = new HashMap<>();

	public Reference getReference(String curie) {
		Reference ref = refCacheMap.get(curie);
		if (ref != null) {
			return ref;
		}
		ObjectResponse<Reference> objectRef = referenceApi.get(curie);
		Reference reference = objectRef.getEntity();
		if (reference == null) {
			throw new RuntimeException("Could not find Reference by curie " + curie);
		}
		refCacheMap.put(curie, reference);
		return reference;
	}

}
