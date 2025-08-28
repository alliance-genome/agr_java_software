package org.alliancegenome.indexer.indexers.curation.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.alliancegenome.curation_api.model.entities.base.AuditedObject;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.repository.VariantRepository;
import org.apache.commons.collections4.CollectionUtils;

import lombok.extern.log4j.Log4j2;
import net.nilosplace.process_display.util.ObjectFileStorage;

@Log4j2
public class BaseService {

	private static HashSet<String> allNeoAlleleIDs;
	private static HashSet<String> allNeoGeneIDs;
	private static HashSet<String> allNeoModelIDs;
	private static HashSet<String> allNeoVariantIDs;

	public HashSet<String> getAllNeoAlleleIDs() {
		if (allNeoAlleleIDs == null) {
			String alleleIdsFileName = "allele_ids.gz";
			List<String> alleleList = readFromCache(alleleIdsFileName, List.class);

			if (CollectionUtils.isNotEmpty(alleleList)) {
				allNeoAlleleIDs = new HashSet<>(alleleList);
			} else {
				AlleleRepository alleleRepository = new AlleleRepository();
				allNeoAlleleIDs = new HashSet<>(alleleRepository.getAllAlleleIDs());
				alleleRepository.close();
				writeToCache(alleleIdsFileName, new ArrayList<>(allNeoAlleleIDs));
			}
		}
		return allNeoAlleleIDs;
	}

	public HashSet<String> getAllNeoGeneIDs() {

		if (allNeoGeneIDs == null) {
			String geneIdsFileName = "gene_ids.gz";
			List<String> geneList = readFromCache(geneIdsFileName, List.class);

			if (CollectionUtils.isNotEmpty(geneList)) {
				allNeoGeneIDs = new HashSet<>(geneList);
			} else {
				GeneRepository geneRepository = new GeneRepository();
				allNeoGeneIDs = new HashSet<>(geneRepository.getAllGeneKeys());
				geneRepository.close();
				writeToCache(geneIdsFileName, new ArrayList<>(allNeoGeneIDs));
			}
		}

		return allNeoGeneIDs;
	}

	public HashSet<String> getAllNeoModelIDs() {
		if (allNeoModelIDs == null) {
			String modelIdsFileName = "model_ids.gz";
			List<String> modelList = readFromCache(modelIdsFileName, List.class);

			if (CollectionUtils.isNotEmpty(modelList)) {
				allNeoModelIDs = new HashSet<>(modelList);
			} else {
				AlleleRepository alleleRepository = new AlleleRepository();
				allNeoModelIDs = new HashSet<>(alleleRepository.getAllModelKeys());
				alleleRepository.close();
				writeToCache(modelIdsFileName, new ArrayList<>(allNeoModelIDs));
			}
		}
		return allNeoModelIDs;
	}

	public HashSet<String> getAllNeoVariantIDs() {
		if (allNeoVariantIDs == null) {
			String variantIdsFileName = "variant_ids.gz";
			List<String> variantList = readFromCache(variantIdsFileName, List.class);

			if (CollectionUtils.isNotEmpty(variantList)) {
				allNeoVariantIDs = new HashSet<>(variantList);
			} else {
				VariantRepository variantRepository = new VariantRepository();
				allNeoVariantIDs = new HashSet<>(variantRepository.getAllVariantKeys());
				variantRepository.close();
				writeToCache(variantIdsFileName, new ArrayList<>(allNeoVariantIDs));
			}
		}
		return allNeoVariantIDs;
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
		for (AuditedObject auditedObject : entitiesToBeValidated) {
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
