package org.alliancegenome.indexer.indexers.curation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.AGMDiseaseAnnotationInterface;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;

import si.mazi.rescu.RestProxyFactory;

public class AGMDiseaseAnnotationService extends BaseDiseaseAnnotationService {

	private AGMDiseaseAnnotationInterface agmApi = RestProxyFactory.createProxy(AGMDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public List<AGMDiseaseAnnotation> getFiltered() {

		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);
		AlleleRepository alleleRepository = new AlleleRepository();
		GeneRepository geneRepository = new GeneRepository();

		List<AGMDiseaseAnnotation> ret = new ArrayList<>();

		int batchSize = 1000;
		int page = 0;
		int pages = 0;
		
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
//		params.put("subject.curie", "MGI:3707539");

		HashSet<String> alleleIds = new HashSet<>(alleleRepository.getAllAlleleIDs());
		HashSet<String> allGeneIDs = new HashSet<>(geneRepository.getAllGeneKeys());
		HashSet<String> allModelIDs = new HashSet<>(alleleRepository.getAllModelKeys());
		do {
			SearchResponse<AGMDiseaseAnnotation> response = agmApi.findForPublic(page, batchSize, params);

			for(AGMDiseaseAnnotation da: response.getResults()) {
				if(isValidEntity(allModelIDs, da.getSubjectCurie())) {
					if (hasValidEntities(da, allGeneIDs, alleleIds, allModelIDs)) {
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
		alleleRepository.close();
		geneRepository.close();
		return ret;
	}

}
