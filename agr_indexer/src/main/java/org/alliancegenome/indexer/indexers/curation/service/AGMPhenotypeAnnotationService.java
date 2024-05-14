package org.alliancegenome.indexer.indexers.curation.service;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AGMPhenotypeAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.AGMPhenotypeAnnotationInterface;
import si.mazi.rescu.RestProxyFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Log4j2
public class AGMPhenotypeAnnotationService extends BaseDiseaseAnnotationService {

	private final AGMPhenotypeAnnotationInterface agmApi = RestProxyFactory.createProxy(AGMPhenotypeAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);
	private VocabularyService vocabService = new VocabularyService();
	private EcoTermService ecoTermService = new EcoTermService();
	private OrganizationService orgService = new OrganizationService();
	private ReferenceService referenceService = new ReferenceService();

	private final String cacheFileName = "agm_disease_annotation.json.gz";

	public List<AGMPhenotypeAnnotation> getFiltered() {
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
		List<AGMPhenotypeAnnotation> ret = new ArrayList<>();
		log.info("Gene IDs #: " + allGeneIDs);
		log.info("AGM IDs #: " + allModelIDs);

		int batchSize = 1000;
		int page = 0;
		int pages;

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
//		params.put("phenotypeAnnotationSubject.modEntityId", "RGD:1333662");
		//params.put("phenotypeAnnotationSubject.modEntityId", "MGI:4829791");

		do {
			SearchResponse<AGMPhenotypeAnnotation> response = agmApi.findForPublic(page, batchSize, params);
			for (AGMPhenotypeAnnotation da : response.getResults()) {
				if (isValidEntity(allGeneIDs, da.getPhenotypeAnnotationSubject().getIdentifier())) {
					ret.add(da);
				}
			}

			if (page == 0) {
				display.startProcess("Pulling AGM PA's from curation", response.getTotalResults());
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
