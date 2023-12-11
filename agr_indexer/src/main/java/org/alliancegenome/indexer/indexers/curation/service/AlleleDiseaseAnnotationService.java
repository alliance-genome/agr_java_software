package org.alliancegenome.indexer.indexers.curation.service;

import java.util.HashMap;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.AlleleDiseaseAnnotationInterface;

import si.mazi.rescu.RestProxyFactory;

public class AlleleDiseaseAnnotationService extends BaseDiseaseAnnotationService {

	private AlleleDiseaseAnnotationInterface alleleApi = RestProxyFactory.createProxy(AlleleDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	
	private String cacheFileName = "allele_disease_annotation.json.gz";
	
	public List<AlleleDiseaseAnnotation> getFiltered() {
		
		List<AlleleDiseaseAnnotation> ret = readFromCache(cacheFileName, AlleleDiseaseAnnotation.class);
		if(ret.size() > 0) return ret;

		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);
		
		int batchSize = 300;
		int page = 0;
		int pages = 0;
		
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		//params.put("subject.curie", "FB:FBal0065871");

		do {
			SearchResponse<AlleleDiseaseAnnotation> response = alleleApi.find(page, batchSize, params);

			for(AlleleDiseaseAnnotation da: response.getResults()) {
				if(isValidEntity(allAlleleIds, da.getSubjectCurie())) {
					if (hasValidEntities(da, allGeneIDs, allAlleleIds, allModelIDs)) {
						ret.add(da);
					}
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
		
		writeToCache(cacheFileName, ret);
		
		return ret;
	}

}
