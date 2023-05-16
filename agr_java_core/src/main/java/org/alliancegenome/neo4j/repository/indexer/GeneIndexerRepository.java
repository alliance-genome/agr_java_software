package org.alliancegenome.neo4j.repository.indexer;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.Neo4jRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GeneIndexerRepository extends Neo4jRepository<Gene>  {

	protected Runtime runtime = Runtime.getRuntime();
	protected DecimalFormat df = new DecimalFormat("#");
	private GeneDocumentCache cache = new GeneDocumentCache();

	public GeneIndexerRepository() { super(Gene.class); }
	
	public GeneDocumentCache getGeneCacheCrossReferencesSynonyms() {
		log.info("Building GeneDocumentCache CrossReferences and Synonyms only");
		
		ExecutorService executor = Executors.newFixedThreadPool(20); // Run all at once
		
		executor.execute(new GetSynonymsThread());
		executor.execute(new GetCrossReferencesThread());
		executor.execute(new GetGeneMapThread());
		
		executor.shutdown();
		while (!executor.isTerminated()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.info("Finished Building GeneDocumentCache");

		return cache;
	}

	public GeneDocumentCache getGeneDocumentCache() {

		log.info("Building GeneDocumentCache");

		ExecutorService executor = Executors.newFixedThreadPool(20); // Run all at once

		executor.execute(new GetGeneMapThread());
		executor.execute(new GetSynonymsThread());
		executor.execute(new GetCrossReferencesThread());
		executor.execute(new GetChromosomesThread());
		executor.execute(new GetSecondaryIdsThread());
		executor.execute(new GetAllelesMapThread());
		executor.execute(new GetSoTermNameMapThread());
		executor.execute(new GetSoTermNameWithParentsMapThread());
		executor.execute(new GetSoTermNameAgrSlimMapThread());
		executor.execute(new GetStrictOrthologySymbolsMapThread());
		executor.execute(new GetDiseasesMapThread());
		executor.execute(new GetDiseasesAgrSlimMapThread());
		executor.execute(new GetDiseasesWithParentsThread());
		executor.execute(new GetModelMapThread());
		executor.execute(new GetPhenotypeStatementMapThread());
		executor.execute(new GetWhereExpressedMapThread());
		executor.execute(new GetExpressionStagesMapThread());
		executor.execute(new GetSubcellularExpressionAgrSlimMapThread());
		executor.execute(new GetSubcellularExpressionWithParentsMapThread());
		executor.execute(new GetAnatomicalExpressionMapThread());
		executor.execute(new GetAnatomicalExpressionWithParentsMapThread());

		executor.execute(new GetGOTermMapThread("biological_process", true, new CacheCallback() {
			public void setCacheResult(Map<String, Set<String>> result) {
				cache.setBiologicalProcessAgrSlim(result);
			}
		}));
		
		executor.execute(new GetGOTermMapThread("cellular_component", true, new CacheCallback() {
			public void setCacheResult(Map<String, Set<String>> result) {
				cache.setCellularComponentAgrSlim(result);
			}
		}));
		
		executor.execute(new GetGOTermMapThread("molecular_function", true, new CacheCallback() {
			public void setCacheResult(Map<String, Set<String>> result) {
				cache.setMolecularFunctionAgrSlim(result);
			}
		}));
		
		executor.execute(new GetGOTermMapThread("biological_process", false, new CacheCallback() {
			public void setCacheResult(Map<String, Set<String>> result) {
				cache.setBiologicalProcessWithParents(result);
			}
		}));
		
		executor.execute(new GetGOTermMapThread("cellular_component", false, new CacheCallback() {
			public void setCacheResult(Map<String, Set<String>> result) {
				cache.setCellularComponentWithParents(result);
			}
		}));
		
		executor.execute(new GetGOTermMapThread("molecular_function", false, new CacheCallback() {
			public void setCacheResult(Map<String, Set<String>> result) {
				cache.setMolecularFunctionWithParents(result);
			}
		}));

		executor.shutdown();
		while (!executor.isTerminated()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.info("Finished Building GeneDocumentCache");

		return cache;

	}

	private class GetGeneMapThread implements Runnable {

		public void run() {
			log.info("Fetching genes");
			String query = " MATCH p1=(species:Species)-[:FROM_SPECIES]-(g:Gene) ";

			query += " OPTIONAL MATCH pSoTerm=(g:Gene)-[:ANNOTATED_TO]-(soTerm:SOTerm)";
			query += " OPTIONAL MATCH p5=(g:Gene)--(:GenomicLocation) ";
			query += " RETURN p1, pSoTerm, p5";

			Iterable<Gene> genes = null;

			genes = query(query);

			Map<String,Gene> geneMap = new HashMap<>();
			for (Gene gene : genes) {
				geneMap.put(gene.getPrimaryKey(), gene);
			}

			cache.setGeneMap(geneMap);
			log.info("Finished Fetching genes");
		}
	}

	private class GetSynonymsThread implements Runnable {
		@Override
		public void run() {
			log.info("Building gene -> synonyms map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:ALSO_KNOWN_AS]-(s:Synonym) ";
			query += " RETURN gene.primaryKey as id, s.name as value ";

			cache.setSynonyms(getMapSetForQuery(query));
			log.info("Finished Building gene -> synonyms map");
		}
	}

	private class GetCrossReferencesThread implements Runnable {
		public void run() {
			log.info("Building gene -> cross references map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
			query += " RETURN gene.primaryKey as id, cr.name as value";

			Map<String, Set<String>> names = getMapSetForQuery(query);

			query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
			query += " RETURN gene.primaryKey as id, cr.localId as value";

			Map<String, Set<String>> localIds = getMapSetForQuery(query);

			Map<String, Set<String>> map = new HashMap<>();
			Set<String> keys = new HashSet<>();

			keys.addAll(names.keySet());
			keys.addAll(localIds.keySet());

			for (String key: keys) {
				Set<String> values = new HashSet<>();
				values.addAll(names.get(key));
				values.addAll(localIds.get(key));
				map.put(key, values);
			}

			cache.setCrossReferences(map);
			log.info("Finished Building gene -> cross references map");
		}
	}

	private class GetChromosomesThread implements Runnable {
		public void run() {
			log.info("Building gene -> chromosome map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:LOCATED_ON]-(c:Chromosome) ";
			query += " RETURN gene.primaryKey as id, c.primaryKey as value ";

			cache.setChromosomes(getMapSetForQuery(query));
			log.info("Finished Building gene -> chromosome map");
		}
	}

	private class GetSecondaryIdsThread implements Runnable {
		public void run() {
			log.info("Building gene -> secondaryId map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:ALSO_KNOWN_AS]-(s:SecondaryId) ";
			query += " RETURN gene.primaryKey as id, s.name as value ";

			cache.setSecondaryIds(getMapSetForQuery(query));
			log.info("Finished Building gene -> secondaryId map");
		}
	}

	private class GetAllelesMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> alleles map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:IS_ALLELE_OF]-(allele:Allele) ";
			query += " RETURN gene.primaryKey as id,allele.symbolTextWithSpecies as value ";

			cache.setAlleles(getMapSetForQuery(query, "id", "value"));
			log.info("Finished Building gene -> alleles map");
		}
	}

	private class GetSoTermNameMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> soTermName map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:ANNOTATED_TO]-(term:SOTerm) ";
			query += " RETURN gene.primaryKey as id, term.name as value";

			cache.setSoTermNames(getMapSetForQuery(query,"id","value"));
			log.info("Finished Building gene -> soTermName map");
		}
	}

	private class GetSoTermNameWithParentsMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> soTermNameWithParents map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:ANNOTATED_TO]-(:SOTerm)-[:IS_A_PART_OF_CLOSURE]->(term:SOTerm) ";
			query += " RETURN gene.primaryKey as id, term.name as value";

			cache.setSoTermNameWithParents(getMapSetForQuery(query,"id","value"));
			log.info("Finished Building gene -> soTermNameWithParents map");
		}
	}

	private class GetSoTermNameAgrSlimMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> soTermNameAgrSlim map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:ANNOTATED_TO]-(:SOTerm)-[:IS_A_PART_OF_CLOSURE]->(term:SOTerm) ";
			query += " WHERE not term.name in ['region'," +
					"'biological_region'," +
					"'sequence_feature']";

			query += " RETURN gene.primaryKey as id, term.name as value";

			cache.setSoTermNameAgrSlim(getMapSetForQuery(query, "id", "value"));
			log.info("Finished Building gene -> soTermNameAgrSlim map");
		}
	}

	private class GetStrictOrthologySymbolsMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> strictOrthologySymbols map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[o:ORTHOLOGOUS]-(orthoGene:Gene) WHERE o.strictFilter = true  ";
			query += "RETURN distinct gene.primaryKey,orthoGene.symbol ";

			cache.setStrictOrthologySymbols(getMapSetForQuery(query, "gene.primaryKey", "orthoGene.symbol"));
			log.info("Finished Building gene -> strictOrthologySymbols map");
		}
	}

	private class GetDiseasesMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> diseases map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:IS_MARKER_FOR|IS_IMPLICATED_IN|IMPLICATED_VIA_ORTHOLOGY|BIOMARKER_VIA_ORTHOLOGY]-(disease:DOTerm) ";
			query += " RETURN distinct gene.primaryKey, disease.nameKey ";

			cache.setDiseases(getMapSetForQuery(query, "gene.primaryKey", "disease.nameKey"));
			log.info("Finished Building gene -> diseases map");

		}
	}

	private class GetDiseasesAgrSlimMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> diseasesAgrSlim map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:IS_MARKER_FOR|IS_IMPLICATED_IN|IMPLICATED_VIA_ORTHOLOGY|BIOMARKER_VIA_ORTHOLOGY]-(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm) ";
			query += " WHERE disease.subset =~ '.*DO_AGR_slim.*' ";
			query += " RETURN distinct gene.primaryKey, disease.nameKey ";

			cache.setDiseasesAgrSlim(getMapSetForQuery(query, "gene.primaryKey", "disease.nameKey"));
			log.info("Finished Building gene -> diseasesAgrSlim map");
		}
	}

	private class GetDiseasesWithParentsThread implements Runnable {
		public void run() {
			log.info("Building gene -> diseasesWithParents map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:IS_MARKER_FOR|IS_IMPLICATED_IN|IMPLICATED_VIA_ORTHOLOGY|BIOMARKER_VIA_ORTHOLOGY]-(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm) ";
			query += " RETURN distinct gene.primaryKey, disease.nameKey ";

			cache.setDiseasesWithParents(getMapSetForQuery(query, "gene.primaryKey", "disease.nameKey"));
			log.info("Finished Building gene -> diseasesAgrSlim map");
		}
	}

	private class GetModelMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> model map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:MODEL_COMPONENT|SEQUENCE_TARGETING_REAGENT]-(feature)--(gene:Gene)";
			query += " RETURN gene.primaryKey as id, model.nameTextWithSpecies as value";

			cache.setModels(getMapSetForQuery(query));
			log.info("Finished Building gene -> model map");
		}
	}

	private class GetPhenotypeStatementMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> phenotypeStatement map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(phenotype:Phenotype) ";
			query += " RETURN distinct gene.primaryKey, phenotype.phenotypeStatement ";
			cache.setPhenotypeStatements(getMapSetForQuery(query, "gene.primaryKey", "phenotype.phenotypeStatement"));
			log.info("Finished Building gene -> phenotypeStatement map");
		}
	}

	private class GetWhereExpressedMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> whereExpressed map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity) ";
			query += " RETURN distinct gene.primaryKey as id, ebe.whereExpressedStatement as value";

			cache.setWhereExpressed(getMapSetForQuery(query, "id", "value"));
			log.info("Finished Building gene -> whereExpressed map");
		}
	}

	private class GetExpressionStagesMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> expressionStages map");
			String query= "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(:BioEntityGeneExpressionJoin)--(stage:Stage) ";
			query += "RETURN distinct gene.primaryKey as id, stage.name as value";

			cache.setExpressionStages(getMapSetForQuery(query));
			log.info("Finsihed Building gene -> expressionStages map");
		}
	}

	private class GetSubcellularExpressionAgrSlimMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> Subcellular Expression Ribbon map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity)-[:CELLULAR_COMPONENT_RIBBON_TERM]->(term:GOTerm) ";
			query +=  " RETURN distinct gene.primaryKey, term.name ";

			cache.setSubcellularExpressionAgrSlim(getMapSetForQuery(query, "gene.primaryKey", "term.name"));
			log.info("Finished Building gene -> expressionStages map");
		}
	}

	private class GetSubcellularExpressionWithParentsMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> Subcellular Expression w/parents map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity)-[:CELLULAR_COMPONENT]-(:GOTerm)-[:IS_A_PART_OF_CLOSURE]->(term:GOTerm) ";
			query +=  " RETURN distinct gene.primaryKey, term.name ";

			cache.setSubcellularExpressionWithParents(getMapSetForQuery(query, "gene.primaryKey", "term.name"));
			log.info("Finshed Building gene -> Subcellular Expression w/parents map");
		}
	}

	private class GetAnatomicalExpressionMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> Expression Anatomy Ribbon map");
			String query = " MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity)-[:ANATOMICAL_RIBBON_TERM]-(term:Ontology) ";
			query +=  " RETURN distinct gene.primaryKey, term.name ";

			cache.setAnatomicalExpression(getMapSetForQuery(query, "gene.primaryKey", "term.name"));
			log.info("Finished Building gene -> Expression Anatomy Ribbon map");
		}
	}

	private class GetAnatomicalExpressionWithParentsMapThread implements Runnable {
		public void run() {
			log.info("Building gene -> Expression Anatomy w/parents map");
			String query = " MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity)-[:ANATOMICAL_STRUCTURE]-(:Ontology)-[:IS_A_PART_OF_CLOSURE]->(term:Ontology) ";
			query +=  " RETURN distinct gene.primaryKey, term.name ";

			cache.setAnatomicalExpressionWithParents(getMapSetForQuery(query, "gene.primaryKey", "term.name"));
			log.info("Finished Building gene -> Expression Anatomy w/parents map");
		}
	}

	private class GetGOTermMapThread implements Runnable {
		private String type; private Boolean slim; private CacheCallback callback;
		
		public GetGOTermMapThread(String type, Boolean slim, CacheCallback callback) { this.type = type; this.slim = slim; this.callback = callback; }
		public void run() {
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(:GOTerm)-[:IS_A_PART_OF_CLOSURE]->(term:GOTerm) ";
			query += "WHERE term.type = $type";

			Map<String,String> params = new HashMap<String,String>();

			if (slim) {
				query += " AND term.subset =~ '.*goslim_agr.*' ";
			}
			query += " RETURN distinct gene.primaryKey, term.name";

			params.put("type", type);

			callback.setCacheResult(getMapSetForQuery(query, "gene.primaryKey", "term.name", params));
		}
	}
	
	public Map<String, Set<String>> getSpeciesCommonNames() {
		return getMapSetForQuery(" MATCH (species:Species) RETURN species.name as id, species.commonNames as value ");
	}
	
	private interface CacheCallback {
		void setCacheResult(Map<String, Set<String>> result);
	}

}
