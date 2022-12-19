package org.alliancegenome.indexer.indexers.curation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneDiseaseAnnotationInterface;
import org.alliancegenome.neo4j.repository.GeneRepository;

import lombok.extern.log4j.Log4j2;
import si.mazi.rescu.RestProxyFactory;

@Log4j2
public class GeneDiseaseAnnotationService {

	private GeneDiseaseAnnotationInterface geneApi = RestProxyFactory.createProxy(GeneDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public List<GeneDiseaseAnnotation> getFiltered() {
		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);
		
		List<GeneDiseaseAnnotation> ret = new ArrayList<>();
		
		GeneRepository geneRepository = new GeneRepository();
		
		HashSet<String> geneIds = new HashSet<>(geneRepository.getAllGeneKeys());

		int batchSize = 360;
		int page = 0;
		int pages;

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		
		do {
			SearchResponse<GeneDiseaseAnnotation> response = geneApi.find(page, batchSize, params);

			for(GeneDiseaseAnnotation da: response.getResults()) {
				if(!da.getInternal() && geneIds.contains(da.getSubject().getCurie())) {
					ret.add(da);
				} else {
					System.out.println("Id not found in Neo: " + da.getSubject().getCurie());
				}
			}

			if(page == 0) {
				display.startProcess("Pulling Gene DA's from curation", response.getTotalResults());
			}
			display.progressProcess(response.getReturnedRecords().longValue());
			pages = (int) (response.getTotalResults() / batchSize);
			page++;
		} while(page <= pages);
		display.finishProcess();
		geneRepository.close();
		
		return ret;
	}
	
	
}
