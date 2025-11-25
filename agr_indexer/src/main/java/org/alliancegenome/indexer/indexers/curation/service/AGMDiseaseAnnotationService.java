package org.alliancegenome.indexer.indexers.curation.service;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.AGMDiseaseAnnotationInterface;
import si.mazi.rescu.RestProxyFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AGMDiseaseAnnotationService extends BaseDiseaseAnnotationService {

	private AGMDiseaseAnnotationInterface agmApi = RestProxyFactory.createProxy(AGMDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private String cacheFileName = "agm_disease_annotation.json.gz";

	public List<AGMDiseaseAnnotation> getFiltered() {

		List<AGMDiseaseAnnotation> ret = readFromCache(cacheFileName, List.class);
		if (ret != null && ret.size() > 0) {
			return ret;
		} else {
			ret = new ArrayList<>();
		}

		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);

		int batchSize = 1000;

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		//params.put("diseaseAnnotationSubject.primaryExternalId", "WBStrain00000001");
		// params.put("diseaseAnnotationSubject.primaryExternalId",
		// "ZFIN:ZDB-FISH-150901-27842");

		SearchResponse<AGMDiseaseAnnotation> totalResponse = agmApi.findForPublic(0, 0, params);
		display.startProcess("Pulling AGM DA's from curation", totalResponse.getTotalResults());

		for (int page = 0; page < (int) (totalResponse.getTotalResults() / batchSize); page++) {
			SearchResponse<AGMDiseaseAnnotation> response = agmApi.findForPublic(page, batchSize, params);
			for (AGMDiseaseAnnotation da : response.getResults()) {
				if (hasNoObsoletedOrInternalEntities(da)) {
					if (da.getInferredGene() != null && da.getInferredGene().getConstructGenomicEntityAssociations() != null) {
						da.getInferredGene().getConstructGenomicEntityAssociations().clear();
					}
					if (da.getAssertedGenes() != null) {
						for (Gene g : da.getAssertedGenes()) {
							if (g.getConstructGenomicEntityAssociations() != null) {
								g.getConstructGenomicEntityAssociations().clear();
							}
						}
					}
					ret.add(da);
				}
			}
			display.progressProcess(response.getReturnedRecords().longValue());
		}
		display.finishProcess();
		writeToCache(cacheFileName, ret);
		return ret;
	}

}
