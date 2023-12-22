package org.alliancegenome.indexer.indexers.curation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.GeneDiseaseAnnotationInterface;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections4.CollectionUtils;

import si.mazi.rescu.RestProxyFactory;

public class GeneDiseaseAnnotationService extends BaseDiseaseAnnotationService {

	private final GeneDiseaseAnnotationInterface geneApi = RestProxyFactory.createProxy(GeneDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public List<GeneDiseaseAnnotation> getFiltered() {
		ProcessDisplayHelper display = new ProcessDisplayHelper(2000);
		List<GeneDiseaseAnnotation> ret = new ArrayList<>();
		GeneRepository geneRepository = new GeneRepository();
		AlleleRepository alleleRepository = new AlleleRepository();
		HashSet<String> alleleIds = new HashSet<>(alleleRepository.getAllAlleleIDs());
		HashSet<String> allGeneIDs = new HashSet<>(geneRepository.getAllGeneKeys());
		HashSet<String> allModelIDs = new HashSet<>(alleleRepository.getAllModelKeys());

		int batchSize = 1000;
		int page = 0;
		int pages;

		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		//params.put("subject.curie", "SGD:S000005450");

		do {
			SearchResponse<GeneDiseaseAnnotation> response = geneApi.findForPublic(page, batchSize, params);
			for(GeneDiseaseAnnotation da: response.getResults()) {
				if(isValidEntity(allGeneIDs, da.getSubjectCurie())) {
					if (hasValidGeneticModifiers(da, allGeneIDs, alleleIds, allModelIDs)) {
						ret.add(da);
					}
				}
			}

			if (page == 0) {
				display.startProcess("Pulling Gene DA's from curation", response.getTotalResults());
			}
			display.progressProcess(response.getReturnedRecords().longValue());
			pages = (int) (response.getTotalResults() / batchSize);
			page++;
		} while (page <= pages);
		display.finishProcess();
		geneRepository.close();
		alleleRepository.close();

		return ret;
	}

	private static boolean hasValidGenes(GeneDiseaseAnnotation da, HashSet<String> allGeneIDs) {
		if (da.getInternal())
			return false;
		if (!allGeneIDs.contains(da.getSubject().getCurie()))
			return false;
		if (CollectionUtils.isNotEmpty(da.getWith())) {

			if (da.getWith().stream().anyMatch((gene -> !allGeneIDs.contains(gene.getCurie()))))
				return false;
		}
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifiers())) {
			if (da.getDiseaseGeneticModifiers().stream()
				.anyMatch((gene -> !allGeneIDs.contains(gene.getCurie()))))
				return false;
		}
		return true;
	}

}
