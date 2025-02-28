package org.alliancegenome.indexer.indexers.curation.service;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AllelePhenotypeAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.AllelePhenotypeAnnotationInterface;
import si.mazi.rescu.RestProxyFactory;

import java.util.HashMap;
import java.util.List;

@Log4j2
public class AllelePhenotypeAnnotationService extends BaseDiseaseAnnotationService {

	private final AllelePhenotypeAnnotationInterface alleleApi = RestProxyFactory.createProxy(AllelePhenotypeAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	private final String cacheFileName = "allele_phenotype_annotation.json.gz";

	public List<AllelePhenotypeAnnotation> getFiltered() {
		List<AllelePhenotypeAnnotation> ret = readFromCache(cacheFileName, List.class);
		if (ret != null && ret.size() > 0) {
			return ret;
		}
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
		log.info("Gene IDs #: " + allGeneIDs);
		log.info("Allele IDs #: " + allAlleleIds);

		int batchSize = 1000;
		int page = 0;
		int pages;

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		//params.put("phenotypeAnnotationSubject.primaryExternalId", "SGD:S000286812");
		//params.put("phenotypeAnnotationSubject.modEntityId", "MGI:4829791");

		do {
			SearchResponse<AllelePhenotypeAnnotation> response = alleleApi.findForPublic(page, batchSize, params);
			for (AllelePhenotypeAnnotation da : response.getResults()) {
				if (isValidEntity(allAlleleIds, da.getPhenotypeAnnotationSubject().getIdentifier())) {
					ret.add(da);
				}
			}

			if (page == 0) {
				display.startProcess("Pulling Allele PA's from curation", response.getTotalResults());
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
