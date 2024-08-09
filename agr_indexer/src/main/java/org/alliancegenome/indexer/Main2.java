package org.alliancegenome.indexer;

import java.util.HashMap;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.indexer.indexers.curation.interfaces.AGMDiseaseAnnotationInterface;

import si.mazi.rescu.RestProxyFactory;

public class Main2 {

	public static void main(String[] args) {

		AGMDiseaseAnnotationInterface agmApi = RestProxyFactory.createProxy(AGMDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

		int batchSize = 1000;
		int page = 0;
		int pages;

		HashMap<String, Object> params = new HashMap<>();
		params.put("modInternalId", "MGI:diseaseannotation_239930138_295748099");
		params.put("internal", false);
		params.put("obsolete", false);
		params.put("debug", "true");
		// params.put("diseaseAnnotationSubject.modEntityId", "RGD:69258");

		SearchResponse<AGMDiseaseAnnotation> response = agmApi.findForPublic(page, batchSize, params);
		
		
		System.out.println(response);

	}

}
