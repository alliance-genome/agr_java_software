package org.alliancegenome.indexer.indexers.curation.service;

import java.util.HashMap;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.AGMDiseaseAnnotationInterface;

import si.mazi.rescu.RestProxyFactory;

public class AGMDiseaseAnnotationService extends BaseDiseaseAnnotationService {

	private AGMDiseaseAnnotationInterface agmApi = RestProxyFactory.createProxy(AGMDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	
	private String cacheFileName = "agm_disease_annotation.json.gz";
	
	public List<AGMDiseaseAnnotation> getFiltered() {
		
		List<AGMDiseaseAnnotation> ret = readFromCache(cacheFileName, AGMDiseaseAnnotation.class);
		if(ret.size() > 0) return ret;

		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);

		int batchSize = 1000;
		int page = 0;
		int pages = 0;
		
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);

		do {
			SearchResponse<AGMDiseaseAnnotation> response = agmApi.findForPublic(page, batchSize, params);

			for(AGMDiseaseAnnotation da: response.getResults()) {
				if(isValidEntity(allModelIDs, da.getSubjectCurie())) {
					if (hasValidEntities(da, allGeneIDs, allAlleleIds, allModelIDs)) {
						if(da.getInferredGene() != null && da.getInferredGene().getConstructGenomicEntityAssociations() != null) {
							da.getInferredGene().getConstructGenomicEntityAssociations().clear();
						}
						if(da.getAssertedGenes() != null) {
							for(Gene g: da.getAssertedGenes()) {
								if(g.getConstructGenomicEntityAssociations() != null) {
									g.getConstructGenomicEntityAssociations().clear();
								}
							}
						}
						ret.add(da);
					}
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

		writeToCache(cacheFileName, ret);
		
		return ret;
	}

}
