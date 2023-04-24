package org.alliancegenome.indexer.indexers.curation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.AlleleDiseaseAnnotationInterface;
import org.alliancegenome.neo4j.repository.AlleleRepository;

import si.mazi.rescu.RestProxyFactory;

public class AlleleDiseaseAnnotationService {

	private AlleleDiseaseAnnotationInterface alleleApi = RestProxyFactory.createProxy(AlleleDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public List<AlleleDiseaseAnnotation> getFiltered() {

		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);
		
		List<AlleleDiseaseAnnotation> ret = new ArrayList<>();

		AlleleRepository alleleRepository = new AlleleRepository();
		
		HashSet<String> alleleIds = new HashSet<>(alleleRepository.getAllAlleleIDs());
		System.out.println("Allele Ids: " + alleleIds.size());
		
		int batchSize = 300;
		int page = 0;
		int pages = 0;
		
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		//params.put("subject.curie", "WB:WBVar00087891");

		do {
			SearchResponse<AlleleDiseaseAnnotation> response = alleleApi.find(page, batchSize, params);
			
			for(AlleleDiseaseAnnotation da: response.getResults()) {
				if(!da.getInternal() && alleleIds.contains(da.getSubject().getCurie())) {
					ret.add(da);
				}
			}
			
			if(page == 0) {
				display.startProcess("Pulling Allele DA's from curation", response.getTotalResults());
			}
			display.progressProcess(response.getReturnedRecords().longValue());
			
			pages = (int) (response.getTotalResults() / batchSize);
			page++;
		} while(page <= pages);
		display.finishProcess();
		alleleRepository.close();
		
		return ret;
	}
	
	
}
