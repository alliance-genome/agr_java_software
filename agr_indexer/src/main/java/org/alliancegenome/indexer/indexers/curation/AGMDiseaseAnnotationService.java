package org.alliancegenome.indexer.indexers.curation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.AgmDiseaseAnnotationInterface;

import si.mazi.rescu.RestProxyFactory;

public class AGMDiseaseAnnotationService {

	private AgmDiseaseAnnotationInterface agmApi = RestProxyFactory.createProxy(AgmDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public List<AGMDiseaseAnnotation> getAll() {

		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);
		
		List<AGMDiseaseAnnotation> ret = new ArrayList<>();

		int batchSize = 1000;
		int page = 0;
		int pages = 0;
		
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		
		do {
			SearchResponse<AGMDiseaseAnnotation> response = agmApi.find(page, batchSize, params);
			
			for(AGMDiseaseAnnotation da: response.getResults()) {
				if(!da.getInternal()) {
					ret.add(da);
				}
			}
			
			if(page == 0) {
				display.startProcess("Pulling AGM DA's from curation", response.getTotalResults());
			}
			display.progressProcess(response.getReturnedRecords().longValue());
			
			pages = (int) (response.getTotalResults() / batchSize);
			page++;
		} while(page <= pages);
		display.finishProcess();

		return ret;
	}

}
