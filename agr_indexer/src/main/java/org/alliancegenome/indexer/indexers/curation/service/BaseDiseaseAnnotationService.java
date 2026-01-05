package org.alliancegenome.indexer.indexers.curation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.BiologicalEntity;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
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
			if (agmda.getAssertedAlleles() != null) {
				entitiesToBeValidated.addAll(agmda.getAssertedAlleles());
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

	protected static boolean hasValidGeneticModifiers(DiseaseAnnotation da) {
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
		return true;
	}

}
