package org.alliancegenome.neo4j.repository.indexer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.es.index.site.cache.AlleleDocumentCache;
import org.alliancegenome.es.util.CollectionHelper;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AlleleIndexerRepository extends AlleleRepository {

	private AlleleDocumentCache cache = new AlleleDocumentCache();

	public AlleleDocumentCache getAlleleDocumentCache() {
		log.info("Building AlleleDocumentCache");

		ExecutorService executor = Executors.newFixedThreadPool(20); // Run all at once

		executor.execute(new GetAlleleVariantsMapThread());
		executor.execute(new GetCrossReferencesThread());
		executor.execute(new GetConstructsThread());
		executor.execute(new GetConstructExpressedComponentsThread());
		executor.execute(new GetConstructKnockdownComponent());
		executor.execute(new GetConstructRegulatoryRegions());
		executor.execute(new GetDiseaseMapThread());
		executor.execute(new GetDiseasesAgrSlimMapThread());
		executor.execute(new GetDiseaseWithParentsThread());
		executor.execute(new GetGenesMapThread());
		executor.execute(new GetGenesIdsMapThread());
		executor.execute(new GetGeneSynonymsThread());
		executor.execute(new GetGeneCrossReferencesThread());
		executor.execute(new GetModelsThread());
		executor.execute(new GetPhenotypeStatementsMapThread());
		executor.execute(new GetVariantsThread());
		executor.execute(new GetVariantSynonymsThread());
		executor.execute(new GetVariantTypeMapThread());
		executor.execute(new GetMolecularConsequence());
		
		executor.shutdown();
		while (!executor.isTerminated()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		log.info("Finished Building AlleleDocumentCache");

		return cache;

	}
	
	private abstract class GetDataThread<T> implements Runnable {
		protected Map<String, T> dataCacheMap;
		protected String cacheFileName;
		
		public GetDataThread(String cacheFileName) {
			this.cacheFileName = cacheFileName;
		}
		
		protected void writeCache() {
			writeToCache(cacheFileName, dataCacheMap);
		}

		@Override
		public void run() {
			dataCacheMap = readFromCache(cacheFileName, Map.class);
			if (dataCacheMap != null && dataCacheMap.size() > 0) {
				log.info(getClass().getSimpleName() + " Data Loaded from file cache");
				setCache();
				return;
			}
			runMethod();
			setCache();
			writeCache();
			log.info(getClass().getSimpleName() + "Data Written to file cache");
		}
		
		protected abstract void runMethod();
		protected abstract void setCache();
	}
	
	
	private class GetAlleleVariantsMapThread extends GetDataThread<Allele> {

		protected GetAlleleVariantsMapThread() {
			super("AlleleVariantsMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Fetching alleles objects");
			dataCacheMap = getAllAlleleVariants();
			log.info("Finished Fetching alleles objects");
		}

		@Override
		protected void setCache() {
			cache.setAlleleMap(dataCacheMap);
		}
	}

	private class GetCrossReferencesThread extends GetDataThread<Set<String>> {

		protected GetCrossReferencesThread() {
			super("CrossReferencesMapCache.data");
		}

		@Override
		protected void runMethod() {

			log.info("Building allele -> crossReference map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
			query += " RETURN allele.primaryKey as id, cr.name as value";

			Map<String, Set<String>> names = getMapSetForQuery(query);

			query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
			query += " RETURN allele.primaryKey as id, cr.localId as value";

			Map<String, Set<String>> localIds = getMapSetForQuery(query);

			dataCacheMap = CollectionHelper.merge(names, localIds);
			log.info("Finished Building allele -> crossReference map");
		}

		@Override
		protected void setCache() {
			cache.setCrossReferences(dataCacheMap);
		}
	}

	private class GetConstructsThread extends GetDataThread<Set<String>> {
		
		public GetConstructsThread() {
			super("ConstructsMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Fetching allele -> constructs map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct) ";
			query += " RETURN allele.primaryKey as id, [construct.nameText, construct.primaryKey] as value";

			dataCacheMap = getMapSetForQuery(query);

			query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";

			query += "RETURN allele.primaryKey as id, synonym.name as value ";

			dataCacheMap = CollectionHelper.merge(dataCacheMap, getMapSetForQuery(query));

			query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:CROSS_REFERENCE]-(crossReference:CrossReference) ";
			query += "RETURN allele.primaryKey as id, crossReference.name as value ";

			dataCacheMap = CollectionHelper.merge(dataCacheMap, getMapSetForQuery(query));

			query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:ALSO_KNOWN_AS]-(secondaryId:SecondaryId) ";

			query += " RETURN allele.primaryKey as id, secondaryId.name as value";

			dataCacheMap = CollectionHelper.merge(dataCacheMap, getMapSetForQuery(query));

			query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:EXPRESSES|TARGETS|IS_REGULATED_BY]-(gene:Gene) ";
			query += " RETURN allele.primaryKey as id, gene.primaryKey as value; ";

			dataCacheMap = CollectionHelper.merge(dataCacheMap, getMapSetForQuery(query));

			log.info("Finished Fetching allele -> constructs map");
		}

		@Override
		protected void setCache() {
			cache.setConstructs(dataCacheMap);
		}
	}

	private class GetConstructExpressedComponentsThread extends GetDataThread<Set<String>> {

		public GetConstructExpressedComponentsThread() {
			super("ConstructExpressedComponentsMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Fetching allele -> constructExpressedComponent map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:EXPRESSES]-(constructComponent:NonBGIConstructComponent) ";
			query += " RETURN allele.primaryKey as id, constructComponent.primaryKey as value";

			Map<String, Set<String>> result = getMapSetForQuery(query);

			query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:EXPRESSES]-(gene:Gene) ";
			query += " RETURN allele.primaryKey as id, gene.symbolWithSpecies as value; ";
			
			dataCacheMap = CollectionHelper.merge(result, getMapSetForQuery(query));

			log.info("Finished Fetching allele -> constructExpressedComponent map");
		}

		@Override
		protected void setCache() {
			cache.setConstructExpressedComponents(dataCacheMap);
		}
	}

	private class GetConstructKnockdownComponent extends GetDataThread<Set<String>> {

		public GetConstructKnockdownComponent() {
			super("ConstructKnockdownComponentMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Fetching allele -> constructKnockdownComponent map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:TARGETS]-(constructComponent:NonBGIConstructComponent) ";
			query += " RETURN allele.primaryKey as id, constructComponent.primaryKey as value";

			Map<String, Set<String>> result = getMapSetForQuery(query);

			query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:TARGETS]-(gene:Gene) ";
			query += " RETURN allele.primaryKey as id, gene.symbolWithSpecies as value; ";

			dataCacheMap = CollectionHelper.merge(result, getMapSetForQuery(query));
			log.info("Finished Fetching allele -> constructKnockdownComponent map");
		}

		@Override
		protected void setCache() {
			cache.setConstructKnockdownComponents(dataCacheMap);
		}
	}
	
	private class GetConstructRegulatoryRegions extends GetDataThread<Set<String>> {

		public GetConstructRegulatoryRegions() {
			super("ConstructRegulatoryRegionsMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Fetching allele -> constructRegulatoryRegion map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:IS_REGULATED_BY]-(constructComponent:NonBGIConstructComponent) ";
			query += " RETURN allele.primaryKey as id, constructComponent.primaryKey as value";

			Map<String, Set<String>> result = getMapSetForQuery(query);

			query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:IS_REGULATED_BY]-(gene:Gene) ";
			query += " RETURN allele.primaryKey as id, gene.symbolWithSpecies as value; ";

			dataCacheMap = CollectionHelper.merge(result, getMapSetForQuery(query));
			log.info("Finished Fetching allele -> constructRegulatoryRegion map");
		}

		@Override
		protected void setCache() {
			cache.setConstructRegulatoryRegions(dataCacheMap);
		}
	}

	private class GetDiseaseMapThread extends GetDataThread<Set<String>> {

		public GetDiseaseMapThread() {
			super("DiseaseMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Building allele -> diseases map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:IS_IMPLICATED_IN]-(disease:DOTerm) ";
			query += " RETURN a.primaryKey, disease.nameKey ";

			dataCacheMap = getMapSetForQuery(query, "a.primaryKey", "disease.nameKey");
			
			log.info("Finished Building allele -> diseases map");
		}

		@Override
		protected void setCache() {
			cache.setDiseases(dataCacheMap);
		}
	}

	private class GetDiseasesAgrSlimMapThread extends GetDataThread<Set<String>> {

		public GetDiseasesAgrSlimMapThread() {
			super("DiseasesAgrSlimMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Building allele -> diseasesAgrSlim map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:IS_IMPLICATED_IN]-(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm)";
			query += " WHERE disease.subset =~ '.*DO_AGR_slim.*' ";
			query += " RETURN a.primaryKey, disease.nameKey ";

			dataCacheMap = getMapSetForQuery(query, "a.primaryKey", "disease.nameKey");
			
			log.info("Finished Building allele -> diseasesAgrSlim map");
		}

		@Override
		protected void setCache() {
			cache.setDiseasesAgrSlim(dataCacheMap);
		}
	}
	
	private class GetDiseaseWithParentsThread extends GetDataThread<Set<String>> {

		public GetDiseaseWithParentsThread() {
			super("DiseaseWithParentsMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Building allele -> diseasesWithParents map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:IS_IMPLICATED_IN]-(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm)";

			query += " RETURN a.primaryKey, disease.nameKey ";

			dataCacheMap = getMapSetForQuery(query, "a.primaryKey", "disease.nameKey");
			log.info("Finished Building allele -> diseasesWithParents map");
		}

		@Override
		protected void setCache() {
			cache.setDiseasesWithParents(dataCacheMap);
		}
	}

	private class GetGenesMapThread extends GetDataThread<Set<String>> {

		public GetGenesMapThread() {
			super("GenesMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Building allele -> genes & gene symbol map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:IS_ALLELE_OF]-(a:Allele) ";
			query += "RETURN distinct a.primaryKey, gene.symbolWithSpecies, gene.primaryKey";

			dataCacheMap = getMapSetForQuery(query, "a.primaryKey", "gene.symbolWithSpecies");

			log.info("Finished Building allele -> genes & gene Ids map");
		}

		@Override
		protected void setCache() {
			cache.setGenes(dataCacheMap);
		}
	}
	
	private class GetGenesIdsMapThread extends GetDataThread<Set<String>> {

		public GetGenesIdsMapThread() {
			super("GenesIdsMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Building allele -> genes & gene Ids map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:IS_ALLELE_OF]-(a:Allele) ";
			query += "RETURN distinct a.primaryKey, gene.symbolWithSpecies, gene.primaryKey";

			dataCacheMap = getMapSetForQuery(query, "a.primaryKey", "gene.primaryKey");

			log.info("Finished Building allele -> genes & gene Ids map");
		}

		@Override
		protected void setCache() {
			cache.setGeneIds(dataCacheMap);
		}
	}

	private class GetGeneSynonymsThread extends GetDataThread<Set<String>> {

		public GetGeneSynonymsThread() {
			super("GeneSynonymsMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Building allele -> genes synonyms map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:IS_ALLELE_OF]-(gene:Gene)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";
			query += "RETURN a.primaryKey, synonym.name";

			dataCacheMap = getMapSetForQuery(query, "a.primaryKey", "synonym.name");
			log.info("Finished Building allele -> genes synonyms map");
		}

		@Override
		protected void setCache() {
			cache.setGeneSynonyms(dataCacheMap);
		}
	}

	private class GetGeneCrossReferencesThread extends GetDataThread<Set<String>> {

		public GetGeneCrossReferencesThread() {
			super("GeneCrossReferencesMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Building allele -> gene crossreferences map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:IS_ALLELE_OF]-(gene:Gene)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
			query += "RETURN a.primaryKey, cr.name";

			dataCacheMap = getMapSetForQuery(query, "a.primaryKey", "cr.name");
			log.info("Finished Building allele -> gene crossreferences map");
		}

		@Override
		protected void setCache() {
			cache.setGeneCrossReferences(dataCacheMap);
		}
	}

	private class GetModelsThread extends GetDataThread<Set<String>> {

		public GetModelsThread() {
			super("ModelsMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Building allele -> model map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:MODEL_COMPONENT]-(allele:Allele)";
			query += " RETURN allele.primaryKey as id, model.nameTextWithSpecies as value";

			dataCacheMap = getMapSetForQuery(query);
			log.info("Finished Building allele -> model map");
		}

		@Override
		protected void setCache() {
			cache.setModels(dataCacheMap);
		}
	}
	
	private class GetPhenotypeStatementsMapThread extends GetDataThread<Set<String>> {

		public GetPhenotypeStatementsMapThread() {
			super("PhenotypeStatementsMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Building allele -> phenotype statements map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:HAS_PHENOTYPE]-(phenotype:Phenotype) ";
			query += " RETURN distinct a.primaryKey, phenotype.phenotypeStatement ";

			dataCacheMap = getMapSetForQuery(query, "a.primaryKey", "phenotype.phenotypeStatement");
			log.info("Finished Building allele -> phenotype statements map");
		}

		@Override
		protected void setCache() {
			cache.setPhenotypeStatements(dataCacheMap);
		}
	}
	
	private class GetVariantsThread extends GetDataThread<Set<String>> {

		public GetVariantsThread() {
			super("VariantsMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Building allele -> variant name map");
			String query = " MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant) ";
			query += " RETURN distinct a.primaryKey as id, v.name as value";

			dataCacheMap = getMapSetForQuery(query);
			log.info("Finished Building allele -> variant name map");
		}

		@Override
		protected void setCache() {
			cache.setVariants(dataCacheMap);
		}
	}
	
	private class GetVariantSynonymsThread extends GetDataThread<Set<String>> {

		public GetVariantSynonymsThread() {
			super("VariantSynonymsMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Building allele -> variant synonyms map");
			String query = " MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:ASSOCIATION]-(tlc:TranscriptLevelConsequence)  ";

			query += "RETURN distinct a.primaryKey as id, [v.hgvsNomenclature, tlc.hgvsVEPGeneNomenclature, tlc.hgvsProteinNomenclature, tlc.hgvsCodingNomenclature] as value  ";
			Map<String, Set<String>> tlcNames = getMapSetForQuery(query);

			query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";

			query += " RETURN a.primaryKey as id, synonym.name as value ";
			Map<String, Set<String>> synonyms = getMapSetForQuery(query);

			dataCacheMap = CollectionHelper.merge(tlcNames, synonyms);
			log.info("Finished Building allele -> variant synonyms map");
		}

		@Override
		protected void setCache() {
			cache.setVariantSynonyms(dataCacheMap);
		}
	}
	
	private class GetVariantTypeMapThread extends GetDataThread<Set<String>> {

		public GetVariantTypeMapThread() {
			super("VariantTypeMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Building allele -> variant types map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:VARIATION_TYPE]-(term:SOTerm) ";
			query += " RETURN distinct a.primaryKey,term.name ";

			dataCacheMap = getMapSetForQuery(query, "a.primaryKey", "term.name");
			log.info("Finished Building allele -> variant types map");
		}

		@Override
		protected void setCache() {
			cache.setVariantType(dataCacheMap);
		}
	}
	
	private class GetMolecularConsequence extends GetDataThread<Set<String>> {

		public GetMolecularConsequence() {
			super("MolecularConsequenceMapCache.data");
		}

		@Override
		protected void runMethod() {
			log.info("Building allele -> molecular consequence map");
			String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:ASSOCIATION]-(consequence:GeneLevelConsequence) ";
			query += " RETURN a.primaryKey as id, consequence.geneLevelConsequence as value ";

			dataCacheMap = getMapSetForQuery(query);
			log.info("Finished Building allele -> molecular consequence map");
		}

		@Override
		protected void setCache() {
			cache.setMolecularConsequenceMap(dataCacheMap);
		}
	}

}
