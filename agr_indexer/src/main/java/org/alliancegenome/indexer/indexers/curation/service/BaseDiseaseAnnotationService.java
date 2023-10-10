package org.alliancegenome.indexer.indexers.curation.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.GenomicEntity;
import org.apache.commons.collections4.CollectionUtils;

public class BaseDiseaseAnnotationService {

	protected boolean hasValidEntities(AGMDiseaseAnnotation da, Set<String> allGeneIDs, Set<String> allAllelIDs, Set<String> allModelIDs) {
		Gene inferredGene = da.getInferredGene();
		List<Gene> assertedGenes = da.getAssertedGenes();
		if (!hasValidInferredAssertedEntities(allGeneIDs, inferredGene, assertedGenes))
			return false;
		Allele inferredAllele = da.getInferredAllele();
		List<Allele> assertedAlleles = null;
		if (da.getAssertedAllele() != null) {
			assertedAlleles = List.of(da.getAssertedAllele());
		}
		if (!hasValidInferredAssertedEntities(allAllelIDs, inferredAllele, assertedAlleles))
			return false;
		return hasValidGeneticModifiers(da, allGeneIDs, allAllelIDs, allModelIDs);
	}

	protected boolean hasValidEntities(AlleleDiseaseAnnotation da, Set<String> allGeneIDs, Set<String> allAllelIDs, Set<String> allModelIDs) {
		Gene inferredGene = da.getInferredGene();
		List<Gene> assertedGenes = da.getAssertedGenes();
		if (!hasValidInferredAssertedEntities(allGeneIDs, inferredGene, assertedGenes))
			return false;
		return hasValidGeneticModifiers(da, allGeneIDs, allAllelIDs, allModelIDs);
	}

	private static boolean hasValidInferredAssertedEntities(Set<String> allEntityIDs, GenomicEntity inferredEntity, List<? extends GenomicEntity> assertedEntity) {
		if (inferredEntity != null && !allEntityIDs.contains(inferredEntity.getCurie()))
			return false;
		if (CollectionUtils.isNotEmpty(assertedEntity)) {
			if (assertedEntity.stream()
				.anyMatch((entity -> !allEntityIDs.contains(entity.getCurie()))))
				return false;
		}
		return true;
	}

	protected static boolean hasValidGeneticModifiers(DiseaseAnnotation da, Set<String> allGeneIDs, Set<String> allAllelIDs, Set<String> allModelIDs) {
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifiers())) {
			if (da.getDiseaseGeneticModifiers().stream()
				.anyMatch((entity -> (!allGeneIDs.contains(entity.getCurie()) &&
					!allAllelIDs.contains(entity.getCurie()) &&
					!allModelIDs.contains(entity.getCurie())))))
				return false;
		}
		return true;
	}

	protected static boolean isValidEntity(HashSet<String> allEntityIds, String curie) {
		return allEntityIds.contains(curie);
	}


}
