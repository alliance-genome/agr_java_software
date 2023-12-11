package org.alliancegenome.indexer.indexers.curation.service;

import java.util.HashMap;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.rest.interfaces.VocabularyRESTInterface;

import si.mazi.rescu.RestProxyFactory;

public class VocabularyService {

	private VocabularyRESTInterface vocabularyApi = RestProxyFactory.createProxy(VocabularyRESTInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	
	private HashMap<String, VocabularyTerm> terms = null;
	
	public HashMap<String, VocabularyTerm> getDiseaseRelationTerms() {
		if(terms != null) return terms;
		
		HashMap<String, Object> params = new HashMap<>();
		params.put("vocabulary.name", "Disease Relation");
		SearchResponse<VocabularyTerm> response = vocabularyApi.find(0, 1000, params);
		terms = new HashMap<>();
		for(VocabularyTerm vt: response.getResults()) {
			terms.put(vt.getName(), vt);
		}
		return terms;
	}

}
