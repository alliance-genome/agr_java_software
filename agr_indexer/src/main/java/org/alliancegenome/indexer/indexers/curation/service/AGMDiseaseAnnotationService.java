package org.alliancegenome.indexer.indexers.curation.service;

import java.util.ArrayList;
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

		List<AGMDiseaseAnnotation> ret = readFromCache(cacheFileName, List.class);
		if (ret != null && ret.size() > 0) {
			return ret;
		} else {
			ret = new ArrayList<>();
		}

		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);

		int batchSize = 1000;
		int page = 0;
		int pages = 0;

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		// params.put("diseaseAnnotationSubject.primaryExternalId", "WB:WBStrain00024340");
		// params.put("diseaseAnnotationSubject.primaryExternalId",
		// "ZFIN:ZDB-FISH-150901-27842");

		do {
			SearchResponse<AGMDiseaseAnnotation> response = agmApi.findForPublic(page, batchSize, params);

			for (AGMDiseaseAnnotation da: response.getResults()) {
				if (isValidNeoEntity(getAllNeoModelIDs(), da.getDiseaseAnnotationSubject().getIdentifier()) && hasNoObsoletedOrInternalEntities(da)) {
					if (hasValidEntities(da, getAllNeoGeneIDs(), getAllNeoAlleleIDs(), getAllNeoModelIDs())) {
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
			}

			display.progressProcess(response.getReturnedRecords().longValue());

			pages = (int) (response.getTotalResults() / batchSize);
			page++;
		} while (page <= pages);
		display.finishProcess();

		writeToCache(cacheFileName, ret);

		return ret;
	}

}
