package org.alliancegenome.indexer.indexers.curation.service;

import lombok.extern.log4j.Log4j2;
import net.nilosplace.process_display.util.ObjectFileStorage;
import org.alliancegenome.curation_api.model.entities.*;
import org.alliancegenome.curation_api.model.entities.base.AuditedObject;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public class BaseDiseaseAnnotationService {

	protected HashSet<String> allAlleleIds;
	protected HashSet<String> allGeneIDs;
	protected HashSet<String> allModelIDs;

	public BaseDiseaseAnnotationService() {
		AlleleRepository alleleRepository = new AlleleRepository();
		GeneRepository geneRepository = new GeneRepository();

		String alleleIdsFileName = "allele_ids.gz";
		List<String> alleleList = readFromCache(alleleIdsFileName, String.class);

		if (CollectionUtils.isNotEmpty(alleleList)) {
			allAlleleIds = new HashSet<>(alleleList);
		} else {
			allAlleleIds = new HashSet<>(alleleRepository.getAllAlleleIDs());
			writeToCache(alleleIdsFileName, new ArrayList<>(allAlleleIds));
		}

		String geneIdsFileName = "gene_ids.gz";
		List<String> geneList = readFromCache(geneIdsFileName, String.class);

		if (CollectionUtils.isNotEmpty(geneList)) {
			allGeneIDs = new HashSet<>(geneList);
		} else {
			allGeneIDs = new HashSet<>(geneRepository.getAllGeneKeys());
			writeToCache(geneIdsFileName, new ArrayList<>(allGeneIDs));
		}
		log.info("Number of all Gene IDs from Neo4j: " + allGeneIDs.size());

		String modelIdsFileName = "model_ids.gz";
		List<String> modelList = readFromCache(modelIdsFileName, String.class);

		if (CollectionUtils.isNotEmpty(modelList)) {
			allModelIDs = new HashSet<>(modelList);
		} else {
			allModelIDs = new HashSet<>(alleleRepository.getAllModelKeys());
			writeToCache(modelIdsFileName, new ArrayList<>(allModelIDs));
		}

		alleleRepository.close();
		geneRepository.close();
	}


	protected boolean hasNoObsoletedEntities(DiseaseAnnotation da) {
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
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifiers())) {
			entitiesToBeValidated.addAll(da.getDiseaseGeneticModifiers());
		}
		AtomicBoolean hasNoObsoletedEntities = new AtomicBoolean(true);
		entitiesToBeValidated.forEach(auditedObject -> {
			if (auditedObject.getObsolete()) {
				hasNoObsoletedEntities.set(false);
			}
		});
		return hasNoObsoletedEntities.get();
	}

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
		if (inferredEntity != null && !allEntityIDs.contains(inferredEntity.getIdentifier()))
			return false;
		if (CollectionUtils.isNotEmpty(assertedEntity)) {
			if (assertedEntity.stream().anyMatch((entity -> !allEntityIDs.contains(entity.getIdentifier()))))
				return false;
		}
		return true;
	}

	protected static boolean hasValidGeneticModifiers(DiseaseAnnotation da, Set<String> allGeneIDs, Set<String> allAllelIDs, Set<String> allModelIDs) {
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifiers())) {
			if (da.getDiseaseGeneticModifiers().stream().anyMatch((entity -> (!allGeneIDs.contains(entity.getIdentifier()) && !allAllelIDs.contains(entity.getIdentifier()) && !allModelIDs.contains(entity.getIdentifier())))))
				return false;
		}
		return true;
	}

	protected static boolean isValidEntity(HashSet<String> allEntityIds, String curie) {
		return allEntityIds.contains(curie);
	}

	protected <E> List<E> readFromCache(String fileName, Class<E> clazz) {
		try {
			ObjectFileStorage<E> storage = new ObjectFileStorage<>();
			File cache = new File(fileName);
			if (cache.exists()) {
				return storage.readObjectsFromFile(cache);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	protected <E> void writeToCache(String fileName, List<E> objects) {
		try {
			ObjectFileStorage<E> storage = new ObjectFileStorage<>();
			storage.writeObjectsToFile(objects, fileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
