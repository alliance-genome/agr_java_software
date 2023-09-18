package org.alliancegenome.indexer.indexers.curation.service;

import org.alliancegenome.curation_api.model.entities.*;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Set;

public class BaseDiseaseAnnotationService {

	protected boolean hasValidEntities(GeneDiseaseAnnotation da, Set<String> allGeneIDs, Set<String> allAllelIDs, Set<String> allModelIDs) {
		return hasValidGeneticModifiers(da, allGeneIDs, allAllelIDs, allModelIDs);
	}

	protected boolean hasValidEntities(AGMDiseaseAnnotation da, Set<String> allGeneIDs, Set<String> allAllelIDs, Set<String> allModelIDs) {
		Gene inferredGene = da.getInferredGene();
		List<Gene> assertedGenes = da.getAssertedGenes();
		return hasValidEntities(da, allGeneIDs, allAllelIDs, allModelIDs, inferredGene, assertedGenes);
	}

	protected boolean hasValidEntities(AlleleDiseaseAnnotation da, Set<String> allGeneIDs, Set<String> allAllelIDs, Set<String> allModelIDs) {
		Gene inferredGene = da.getInferredGene();
		List<Gene> assertedGenes = da.getAssertedGenes();
		return hasValidEntities(da, allGeneIDs, allAllelIDs, allModelIDs, inferredGene, assertedGenes);
	}

	private static boolean hasValidEntities(DiseaseAnnotation da, Set<String> allGeneIDs, Set<String> allAllelIDs, Set<String> allModelIDs, Gene inferredGene, List<Gene> assertedGenes) {
		if (!allGeneIDs.contains(inferredGene.getCurie()))
			return false;
		if (CollectionUtils.isNotEmpty(assertedGenes)) {
			if (assertedGenes.stream()
				.anyMatch((gene -> !allGeneIDs.contains(gene.getCurie()))))
				return false;
		}
		return hasValidGeneticModifiers(da, allGeneIDs, allAllelIDs, allModelIDs);
	}

	private static boolean hasValidGeneticModifiers(DiseaseAnnotation da, Set<String> allGeneIDs, Set<String> allAllelIDs, Set<String> allModelIDs) {
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifiers())) {
			if (da.getDiseaseGeneticModifiers().stream()
				.anyMatch((entity -> (!allGeneIDs.contains(entity.getCurie()) &&
					!allAllelIDs.contains(entity.getCurie()) &&
					!allModelIDs.contains(entity.getCurie())))))
				return false;
		}
		return true;
	}


}
