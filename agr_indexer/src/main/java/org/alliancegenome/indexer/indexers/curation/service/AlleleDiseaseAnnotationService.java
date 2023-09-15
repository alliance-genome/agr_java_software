package org.alliancegenome.indexer.indexers.curation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.alliancegenome.indexer.RestConfig;
import org.alliancegenome.indexer.indexers.curation.interfaces.AlleleDiseaseAnnotationInterface;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections4.CollectionUtils;

import si.mazi.rescu.RestProxyFactory;

public class AlleleDiseaseAnnotationService {

	private AlleleDiseaseAnnotationInterface alleleApi = RestProxyFactory.createProxy(AlleleDiseaseAnnotationInterface.class, ConfigHelper.getCurationApiUrl(), RestConfig.config);

	public List<AlleleDiseaseAnnotation> getFiltered() {

		ProcessDisplayHelper display = new ProcessDisplayHelper(10000);
		List<AlleleDiseaseAnnotation> ret = new ArrayList<>();
		AlleleRepository alleleRepository = new AlleleRepository();
		GeneRepository geneRepository = new GeneRepository();

		int batchSize = 300;
		int page = 0;
		int pages = 0;
		
		HashMap<String, Object> params = new HashMap<>();
		params.put("internal", false);
		params.put("obsolete", false);
		//params.put("subject.curie", "WB:WBVar00087891");

		do {
			SearchResponse<AlleleDiseaseAnnotation> response = alleleApi.find(page, batchSize, params);
			HashSet<String> alleleIds = new HashSet<String>(alleleRepository.getAllAlleleIDs());
			HashSet<String> allGeneIDs = new HashSet<String>(geneRepository.getAllGeneKeys());
			
			for(AlleleDiseaseAnnotation da: response.getResults()) {
				if(!da.getInternal() && alleleIds.contains(da.getSubject().getCurie())) {
					if (hasValidGenes(da, allGeneIDs)) {
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
		alleleRepository.close();
		geneRepository.close();
		return ret;
	}

	private boolean hasValidGenes(AlleleDiseaseAnnotation da, HashSet<String> allGeneIDs) {
		
		if (da.getInferredGene() != null && !allGeneIDs.contains(da.getInferredGene().getCurie()))
			return false;
		if (CollectionUtils.isNotEmpty(da.getAssertedGenes())) {
			if (da.getAssertedGenes().stream()
				.anyMatch((gene -> !allGeneIDs.contains(gene.getCurie()))))
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
