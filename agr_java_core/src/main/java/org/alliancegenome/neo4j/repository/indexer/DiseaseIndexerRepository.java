package org.alliancegenome.neo4j.repository.indexer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.es.index.site.cache.DiseaseDocumentCache;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.Neo4jRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiseaseIndexerRepository extends Neo4jRepository<DOTerm> {

	private Logger log = LogManager.getLogger(getClass());

	private DiseaseDocumentCache cache = new DiseaseDocumentCache();
	
	public DiseaseIndexerRepository() { super(DOTerm.class); }


	public DiseaseDocumentCache getDiseaseDocumentCache() {
		
		log.info("Building DiseaseDocumentCache");

		ExecutorService executor = Executors.newFixedThreadPool(20); // Run all at once
		
		executor.execute(new GetDiseaseMapThread());
		executor.execute(new GetGeneMapThread());
		executor.execute(new GetAllelesMapThread());
		executor.execute(new GetModelMapThread());
		executor.execute(new GetSpeciesMapThread());
		executor.execute(new GetSecondaryIdMapThread());
		executor.execute(new GetParentNameMapThread());
		executor.execute(new GetDiseaseGroupMapThread());
		
		executor.shutdown();
		while (!executor.isTerminated()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.info("Finished Building DiseaseDocumentCache");

		return cache;

	}
	
	private class GetDiseaseMapThread implements Runnable {
		@Override
		public void run() {
			log.info("Fetching diseases");
			String query = "MATCH pDisease=(disease:DOTerm) WHERE disease.isObsolete = 'false' ";
			query += " OPTIONAL MATCH pSyn=(disease:DOTerm)-[:ALSO_KNOWN_AS]-(:Synonym) ";
			query += " OPTIONAL MATCH pCR=(disease:DOTerm)-[:CROSS_REFERENCE]-(:CrossReference)";
			query += " RETURN pDisease, pSyn, pCR";

			Iterable<DOTerm> diseases = query(query);

			Map<String,DOTerm> diseaseMap = new HashMap<>();
			for (DOTerm disease : diseases) {
				diseaseMap.put(disease.getPrimaryKey(), disease);
			}
			cache.setDiseaseMap(diseaseMap);
			log.info("Finished Fetching diseases");
		}
	}

	private class GetGeneMapThread implements Runnable {
		@Override
		public void run() {
			log.info("Building disease -> gene map");
			cache.setGenes(getMapSetForQuery("MATCH (disease:DOTerm)-[:IS_MARKER_FOR|:IS_IMPLICATED_IN|:IMPLICATED_VIA_ORTHOLOGY|:BIOMARKER_VIA_ORTHOLOGY]-(gene:Gene) " +
					" RETURN disease.primaryKey as id, gene.symbolWithSpecies as value;"));
			log.info("Finished Building disease -> gene map");
		}
	}

	private class GetAllelesMapThread implements Runnable {
		@Override
		public void run() {
			log.info("Building disease -> allele map");
			cache.setAlleles(getMapSetForQuery("MATCH (disease:DOTerm)-[:IS_IMPLICATED_IN]-(allele:Allele) " +
					"RETURN disease.primaryKey as id, allele.symbolTextWithSpecies as value;"));
			log.info("Finished Building disease -> allele map");
		}
	}
	
	private class GetModelMapThread implements Runnable {
		@Override
		public void run() {
			log.info("Building disease -> model map");
			cache.setModels(getMapSetForQuery("MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:IS_MODEL_OF]-(disease:DOTerm)"
					+ " RETURN disease.primaryKey as id, model.nameTextWithSpecies as value"));
			log.info("Finished Building disease -> model map");
		}
	}

	private class GetSpeciesMapThread implements Runnable {
		@Override
		public void run() {
			log.info("Building disease -> species map");
			cache.setSpeciesMap(getMapSetForQuery("MATCH (disease:DOTerm)-[:IS_MARKER_FOR|:IS_IMPLICATED_IN|:IMPLICATED_VIA_ORTHOLOGY|:BIOMARKER_VIA_ORTHOLOGY]-(gene:Gene)-[:FROM_SPECIES]-(species:Species) " +
					" RETURN disease.primaryKey as id, species.name as value;"));
			log.info("Finished Building disease -> species map");
		}
	}

	private class GetSecondaryIdMapThread implements Runnable {
		@Override
		public void run() {
			log.info("Building disease -> secondaryId map");
			cache.setSecondaryIds(getMapSetForQuery("MATCH (disease:DOTerm)-[:ALSO_KNOWN_AS]-(sid:SecondaryId) " +
					" RETURN disease.primaryKey as id, sid.primaryKey as value"));
			log.info("Finished Building disease -> secondaryId map");
		}
	}

	private class GetParentNameMapThread implements Runnable {
		@Override
		public void run() {
			log.info("Building disease -> disease parent name map");
			cache.setParentNameMap(getMapSetForQuery("MATCH (disease:DOTerm)-[:IS_A_PART_OF_CLOSURE]-(parent:DOTerm) " +
					"RETURN disease.primaryKey as id, disease.nameKey, parent.nameKey as value;"));
			log.info("Finished Building disease -> disease parent name map");
		}
	}

	private class GetDiseaseGroupMapThread implements Runnable {
		@Override
		public void run() {
			log.info("Building disease -> diseaseGroup map");
			cache.setDiseaseGroupMap(getMapSetForQuery("MATCH (disease:DOTerm)-[:IS_A_PART_OF_CLOSURE]-(parent:DOTerm) " +
					"WHERE parent.subset =~ '.*DO_AGR_slim.*' " +
					"RETURN disease.primaryKey as id, parent.nameKey as value;"));
			log.info("Finished Building disease -> diseaseGroup map");
		}
	}

}
