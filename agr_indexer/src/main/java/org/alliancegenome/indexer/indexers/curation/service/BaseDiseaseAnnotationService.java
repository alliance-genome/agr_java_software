package org.alliancegenome.indexer.indexers.curation.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alliancegenome.curation_api.model.entities.AGMDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Allele;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.GenomicEntity;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections4.CollectionUtils;

import net.nilosplace.process_display.util.ObjectFileStorage;

public class BaseDiseaseAnnotationService {
	
	protected HashSet<String> allAlleleIds;
	protected HashSet<String> allGeneIDs;
	protected HashSet<String> allModelIDs;
	
	public BaseDiseaseAnnotationService() {
		AlleleRepository alleleRepository = new AlleleRepository();
		GeneRepository geneRepository = new GeneRepository();
		
		List<String> alleleList = readFromCache("allele_ids.gz", String.class);
		
		if(alleleList == null) {
			allAlleleIds = new HashSet<>(alleleList);
		} else {
			allAlleleIds = new HashSet<>(alleleRepository.getAllAlleleIDs());
			writeToCache("allele_ids.gz", new ArrayList<>(allAlleleIds));
		}
		
		List<String> geneList = readFromCache("gene_ids.gz", String.class);
		
		if(geneList == null) {
			allGeneIDs = new HashSet<>(geneList);
		} else {
			allGeneIDs = new HashSet<>(geneRepository.getAllGeneKeys());
			writeToCache("gene_ids.gz", new ArrayList<>(allGeneIDs));
		}
		
		List<String> modelList = readFromCache("model_ids.gz", String.class);
		
		if(modelList == null) {
			allModelIDs = new HashSet<>(modelList);
		} else {
			allModelIDs = new HashSet<>(alleleRepository.getAllModelKeys());
			writeToCache("model_ids.gz", new ArrayList<>(allModelIDs));
		}
		
		alleleRepository.close();
		geneRepository.close();
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
		if (inferredEntity != null && !allEntityIDs.contains(inferredEntity.getCurie()))
			return false;
		if (CollectionUtils.isNotEmpty(assertedEntity)) {
			if (assertedEntity.stream().anyMatch((entity -> !allEntityIDs.contains(entity.getCurie()))))
				return false;
		}
		return true;
	}

	protected static boolean hasValidGeneticModifiers(DiseaseAnnotation da, Set<String> allGeneIDs, Set<String> allAllelIDs, Set<String> allModelIDs) {
		if (CollectionUtils.isNotEmpty(da.getDiseaseGeneticModifiers())) {
			if (da.getDiseaseGeneticModifiers().stream().anyMatch((entity -> (!allGeneIDs.contains(entity.getCurie()) && !allAllelIDs.contains(entity.getCurie()) && !allModelIDs.contains(entity.getCurie())))))
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
