package org.alliancegenome.indexer.indexers.curation.service;

import java.util.HashMap;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.rest.interfaces.VocabularyRESTInterface;

import lombok.Getter;
import si.mazi.rescu.RestProxyFactory;

public class VocabularyService {

	private VocabularyRESTInterface vocabularyApi = RestProxyFactory.createProxy(VocabularyRESTInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

/*
	public List<Vocabulary> getAllVocabularies() {
		SearchResponse<Vocabulary> response = vocabularyApi.find(0, 1000, new HashMap<>());
		return response.getResults();
	}
*/

	public List<VocabularyTerm> getAllVocabularyTerms() {
		SearchResponse<VocabularyTerm> response = vocabularyApi.find(0, 1000, new HashMap<>());
		return response.getResults();
	}

	@Getter(lazy = true)
	private final List<VocabularyTerm> terms = getAllVocabularyTerms();

	public VocabularyTerm getVocabularyTerm(String name) {
		return getTerms().stream().filter(vocabularyTerm -> vocabularyTerm.getName().equals(name)).findFirst().get();
	}
}
