package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneDiseaseAnnotationInterface;
import org.alliancegenome.neo4j.repository.GeneRepository;

import si.mazi.rescu.RestProxyFactory;

public class GeneDiseaseAnnotationService {

	private GeneDiseaseAnnotationInterface geneApi = RestProxyFactory.createProxy(GeneDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	
	private static GeneRepository geneRepository = new GeneRepository();

	public List<GeneDiseaseAnnotation> getFiltered() {
		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);
		
		List<GeneDiseaseAnnotation> ret = new ArrayList<>();
		
		List<String> geneIds = geneRepository.getAllGeneKeys();
		
		int batchSize = 1000;
		int page = 0;
		int pages = 0;

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		
		do {
			SearchResponse<GeneDiseaseAnnotation> response = geneApi.find(page, batchSize, params);

			for(GeneDiseaseAnnotation da: response.getResults()) {
				if(!da.getInternal() && geneIds.contains(da.getSubject().getCurie())) {
					ret.add(da);
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
		
		return ret;
	}
	
	
}
