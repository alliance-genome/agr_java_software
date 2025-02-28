package org.alliancegenome.indexer.indexers.curation.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.alliancegenome.curation_api.model.entities.base.AuditedObject;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections4.CollectionUtils;

import lombok.extern.log4j.Log4j2;
import net.nilosplace.process_display.util.ObjectFileStorage;

@Log4j2
public class BaseService {

	protected HashSet<String> allNeoAlleleIDs;
	protected HashSet<String> allNeoGeneIDs;
	protected HashSet<String> allNeoModelIDs;

	public BaseService() {
		AlleleRepository alleleRepository = new AlleleRepository();
		GeneRepository geneRepository = new GeneRepository();

		String alleleIdsFileName = "allele_ids.gz";
		List<String> alleleList = readFromCache(alleleIdsFileName, List.class);

		if (CollectionUtils.isNotEmpty(alleleList)) {
			allNeoAlleleIDs = new HashSet<>(alleleList);
		} else {
			allNeoAlleleIDs = new HashSet<>(alleleRepository.getAllAlleleIDs());
			writeToCache(alleleIdsFileName, new ArrayList<>(allNeoAlleleIDs));
		}

		String geneIdsFileName = "gene_ids.gz";
		List<String> geneList = readFromCache(geneIdsFileName, List.class);

		if (CollectionUtils.isNotEmpty(geneList)) {
			allNeoGeneIDs = new HashSet<>(geneList);
		} else {
			allNeoGeneIDs = new HashSet<>(geneRepository.getAllGeneKeys());
			writeToCache(geneIdsFileName, new ArrayList<>(allNeoGeneIDs));
		}
		log.info("Number of all Gene IDs from Neo4j: " + allNeoGeneIDs.size());

		String modelIdsFileName = "model_ids.gz";
		List<String> modelList = readFromCache(modelIdsFileName, List.class);

		if (CollectionUtils.isNotEmpty(modelList)) {
			allNeoModelIDs = new HashSet<>(modelList);
		} else {
			allNeoModelIDs = new HashSet<>(alleleRepository.getAllModelKeys());
			writeToCache(modelIdsFileName, new ArrayList<>(allNeoModelIDs));
		}

		alleleRepository.close();
		geneRepository.close();
	}

	protected <E> E readFromCache(String fileName, Class<E> clazz) {
		try {
			ObjectFileStorage<E> storage = new ObjectFileStorage<>();
			File cache = new File(fileName);
			if (cache.exists()) {
				return storage.readObjectFromFile(cache);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected <E> void writeToCache(String fileName, E object) {
		try {
			ObjectFileStorage<E> storage = new ObjectFileStorage<>();
			storage.writeObjectToFile(object, fileName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected boolean hasNoExcludedEntities(List<AuditedObject> entitiesToBeValidated) {
		AtomicBoolean hasNoExcludedEntities = new AtomicBoolean(true);
		for (AuditedObject auditedObject: entitiesToBeValidated) {
			if (auditedObject.getObsolete()) {
				hasNoExcludedEntities.set(false);
			}
			if (auditedObject.getInternal()) {
				hasNoExcludedEntities.set(false);
			}
		}
		return hasNoExcludedEntities.get();
	}

	protected static boolean isValidNeoEntity(HashSet<String> neoEntityIds, String curie) {
		return neoEntityIds.contains(curie);
	}

}
