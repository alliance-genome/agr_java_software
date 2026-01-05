package org.alliancegenome.indexer.indexers.curation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.AlleleDiseaseAnnotationInterface;

import si.mazi.rescu.RestProxyFactory;

public class AlleleDiseaseAnnotationService extends BaseDiseaseAnnotationService {

	private AlleleDiseaseAnnotationInterface alleleApi = RestProxyFactory.createProxy(AlleleDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private String cacheFileName = "allele_disease_annotation.json.gz";

	public List<AlleleDiseaseAnnotation> getFiltered() {

		List<AlleleDiseaseAnnotation> ret = readFromCache(cacheFileName, List.class);
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
		//params.put("diseaseAnnotationSubject.primaryExternalId", "WB:WBVar00266693");

		SearchResponse<AlleleDiseaseAnnotation> totalResponse = alleleApi.findForPublic(0, 0, params);
		display.startProcess("Pulling Allele DA's from curation", totalResponse.getTotalResults());

		for (int page = 0; page < (int) (totalResponse.getTotalResults() / batchSize); page++) {

			SearchResponse<AlleleDiseaseAnnotation> response = alleleApi.findForPublic(page, batchSize, params);
			for (AlleleDiseaseAnnotation da : response.getResults()) {
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
