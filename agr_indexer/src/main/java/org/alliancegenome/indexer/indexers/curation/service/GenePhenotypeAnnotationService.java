package org.alliancegenome.indexer.indexers.curation.service;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.GenePhenotypeAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GenePhenotypeAnnotationInterface;
import si.mazi.rescu.RestProxyFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Log4j2
public class GenePhenotypeAnnotationService extends BaseDiseaseAnnotationService {

	private final GenePhenotypeAnnotationInterface geneApi = RestProxyFactory.createProxy(GenePhenotypeAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private final String cacheFileName = "gene_phenotype_annotation.json.gz";

	public List<GenePhenotypeAnnotation> getFiltered() {
		List<GenePhenotypeAnnotation> ret = readFromCache(cacheFileName, List.class);
		if (ret != null && ret.size() > 0) {
			return ret;
		} else {
			ret = new ArrayList<>();
		}

		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);

		int batchSize = 1000;
		int page = 0;
		int pages;

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		//params.put("phenotypeAnnotationSubject.primaryExternalId", "SGD:S000001240");
		do {
			SearchResponse<GenePhenotypeAnnotation> response = geneApi.findForPublic(page, batchSize, params);
			for (GenePhenotypeAnnotation pa : response.getResults()) {
				if (isValidNeoEntity(allNeoGeneIDs, pa.getPhenotypeAnnotationSubject().getIdentifier())) {
					ret.add(pa);
				}
			}

			if (page == 0) {
				display.startProcess("Pulling Gene PA's from curation", response.getTotalResults());
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
