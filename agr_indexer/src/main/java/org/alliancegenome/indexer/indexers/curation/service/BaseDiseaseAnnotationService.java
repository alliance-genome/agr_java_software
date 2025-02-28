package org.alliancegenome.indexer.indexers.curation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.BiologicalEntity;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.GenomicEntity;
import org.alliancegenome.curation_api.model.entities.base.AuditedObject;
import org.apache.commons.collections4.CollectionUtils;

public class BaseDiseaseAnnotationService extends BaseService {

	protected boolean hasNoObsoletedOrInternalEntities(DiseaseAnnotation da) {
		List<AuditedObject> entitiesToBeValidated = new ArrayList<>();
		if (da instanceof GeneDiseaseAnnotation gda) {
			entitiesToBeValidated.add(gda.getDiseaseAnnotationSubject());
			if (gda.getSgdStrainBackground() != null) {
				entitiesToBeValidated.add(gda.getSgdStrainBackground());
			}
		} else if (da instanceof AlleleDiseaseAnnotation ada) {
			entitiesToBeValidated.add(ada.getDiseaseAnnotationSubject());
			if (ada.getInferredGene() != null) {
				entitiesToBeValidated.add(ada.getInferredGene());
			}
			if (CollectionUtils.isNotEmpty(ada.getAssertedGenes())) {
				entitiesToBeValidated.addAll(ada.getAssertedGenes());
			}
		} else if (da instanceof AGMDiseaseAnnotation agmda) {
			entitiesToBeValidated.add(agmda.getDiseaseAnnotationSubject());
			if (agmda.getInferredGene() != null) {
				entitiesToBeValidated.add(agmda.getInferredGene());
			}
			if (CollectionUtils.isNotEmpty(agmda.getAssertedGenes())) {
				entitiesToBeValidated.addAll(agmda.getAssertedGenes());
			}
			if (agmda.getInferredAllele() != null) {
				entitiesToBeValidated.add(agmda.getInferredAllele());
			}
			if (agmda.getAssertedAllele() != null) {
				entitiesToBeValidated.add(agmda.getAssertedAllele());
			}
		}
		if (CollectionUtils.isNotEmpty(da.getWith())) {
			entitiesToBeValidated.addAll(da.getWith());
		}
		entitiesToBeValidated.add(da.getDiseaseAnnotationObject());
		List<BiologicalEntity> geneticModifiers = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifierAlleles())) {
			geneticModifiers.addAll(da.getDiseaseGeneticModifierAlleles().stream().filter(Objects::nonNull).toList());
		}
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifierGenes())) {
			geneticModifiers.addAll(da.getDiseaseGeneticModifierGenes().stream().filter(Objects::nonNull).toList());
		}
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifierAgms())) {
			geneticModifiers.addAll(da.getDiseaseGeneticModifierAgms().stream().filter(Objects::nonNull).toList());
		}
		if (CollectionUtils.isNotEmpty(geneticModifiers)) {
			entitiesToBeValidated.addAll(geneticModifiers);
		}
		
		return hasNoExcludedEntities(entitiesToBeValidated);
	}

	protected boolean hasValidEntities(AGMDiseaseAnnotation da, Set<String> allGeneIDs, Set<String> allAllelIDs, Set<String> allModelIDs) {
		Gene inferredGene = da.getInferredGene();
		List<Gene> assertedGenes = da.getAssertedGenes();
		if (!hasValidInferredAssertedEntities(allGeneIDs, inferredGene, assertedGenes)) {
			return false;
		}
		Allele inferredAllele = da.getInferredAllele();
		List<Allele> assertedAlleles = null;
		if (da.getAssertedAllele() != null) {
			assertedAlleles = List.of(da.getAssertedAllele());
		}
		if (!hasValidInferredAssertedEntities(allAllelIDs, inferredAllele, assertedAlleles)) {
			return false;
		}
		return hasValidGeneticModifiers(da, allGeneIDs, allAllelIDs, allModelIDs);
	}

	protected boolean hasValidEntities(AlleleDiseaseAnnotation da, Set<String> allGeneIDs, Set<String> allAllelIDs, Set<String> allModelIDs) {
		Gene inferredGene = da.getInferredGene();
		List<Gene> assertedGenes = da.getAssertedGenes();
		if (!hasValidInferredAssertedEntities(allGeneIDs, inferredGene, assertedGenes)) {
			return false;
		}
		return hasValidGeneticModifiers(da, allGeneIDs, allAllelIDs, allModelIDs);
	}

	private static boolean hasValidInferredAssertedEntities(Set<String> allEntityIDs, GenomicEntity inferredEntity, List<? extends GenomicEntity> assertedEntity) {
		if (inferredEntity != null && !allEntityIDs.contains(inferredEntity.getIdentifier())) {
			return false;
		}
		if (CollectionUtils.isNotEmpty(assertedEntity)) {
			if (assertedEntity.stream().anyMatch(entity -> !allEntityIDs.contains(entity.getIdentifier()))) {
				return false;
			}
		}
		return true;
	}

	protected static boolean hasValidGeneticModifiers(DiseaseAnnotation da, Set<String> allGeneIDs, Set<String> allAllelIDs, Set<String> allModelIDs) {
		List<BiologicalEntity> geneticModifiers = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifierAlleles())) {
			geneticModifiers.addAll(da.getDiseaseGeneticModifierAlleles().stream().filter(Objects::nonNull).toList());
		}
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifierGenes())) {
			geneticModifiers.addAll(da.getDiseaseGeneticModifierGenes().stream().filter(Objects::nonNull).toList());
		}
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifierAgms())) {
			geneticModifiers.addAll(da.getDiseaseGeneticModifierAgms().stream().filter(Objects::nonNull).toList());
		}
		if (CollectionUtils.isNotEmpty(geneticModifiers)) {
			if (geneticModifiers.stream().anyMatch(entity -> !allGeneIDs.contains(entity.getIdentifier()) && !allAllelIDs.contains(entity.getIdentifier()) && !allModelIDs.contains(entity.getIdentifier()))) {
				return false;
			}
		}
		return true;
	}

}
