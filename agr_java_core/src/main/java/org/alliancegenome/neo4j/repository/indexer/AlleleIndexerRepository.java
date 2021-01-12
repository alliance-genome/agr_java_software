package org.alliancegenome.neo4j.repository.indexer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alliancegenome.es.index.site.cache.AlleleDocumentCache;
import org.alliancegenome.es.util.CollectionHelper;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.Neo4jRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AlleleIndexerRepository extends Neo4jRepository<Allele> {

    private final Logger log = LogManager.getLogger(getClass());

    private AlleleDocumentCache cache = new AlleleDocumentCache();
    
    public AlleleIndexerRepository() { super(Allele.class); }

    public AlleleDocumentCache getAlleleDocumentCache(String species) {
        log.info("Building AlleleDocumentCache");

        ExecutorService executor = Executors.newFixedThreadPool(20); // Run all at once
        
        executor.execute(new GetAlleleMapThread(species));
        executor.execute(new GetCrossReferencesThread(species));
        executor.execute(new GetConstructsThread(species));
        executor.execute(new GetConstructExpressedComponentsThread(species));
        executor.execute(new GetConstructKnockdownComponent(species));
        executor.execute(new GetConstructRegulatoryRegions(species));
        executor.execute(new GetDiseaseMapThread(species));
        executor.execute(new GetDiseasesAgrSlimMapThread(species));
        executor.execute(new GetDiseaseWithParentsThread(species));
        executor.execute(new GetGenesMapThread(species));
        executor.execute(new GetModelsThread(species));
        executor.execute(new GetPhenotypeStatementsMapThread(species));
        executor.execute(new GetVariantsThread(species));
        executor.execute(new GetVariantSynonymsThread(species));
        executor.execute(new GetVariantTypeMapThread(species));
        executor.execute(new GetMolecularConsequence(species));
        
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
    
    private class GetAlleleMapThread implements Runnable {
        private String species;

        public GetAlleleMapThread(String species) {
            this.species = species;
        }

        @Override
        public void run() {
            log.info("Fetching alleles");
            String query = "MATCH p1=(species:Species)-[:FROM_SPECIES]-(feature:Allele) ";
            query += getSpeciesWhere(species);
            query += " OPTIONAL MATCH pSyn=(feature:Feature)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";
            query += " RETURN p1, pSyn ";

            Iterable<Allele> alleles = null;

            if (species != null) {
                alleles = query(query, getSpeciesParams(species));
            } else {
                alleles = query(query);
            }

            Map<String, Allele> alleleMap = new HashMap<>();
            for (Allele allele : alleles) {
                alleleMap.put(allele.getPrimaryKey(),allele);
            }
            cache.setAlleleMap(alleleMap);
            log.info("Finished Fetching alleles");
        }
    }
    
    private class GetCrossReferencesThread implements Runnable {
        private String species;

        public GetCrossReferencesThread(String species) {
            this.species = species;
        }

        @Override
        public void run() {
            log.info("Building allele -> crossReference map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
            query += getSpeciesWhere(species);
            query += " RETURN allele.primaryKey as id, cr.name as value";

            Map<String, Set<String>> names = getMapSetForQuery(query, getSpeciesParams(species));

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
            query += getSpeciesWhere(species);
            query += " RETURN allele.primaryKey as id, cr.localId as value";

            Map<String, Set<String>> localIds = getMapSetForQuery(query, getSpeciesParams(species));

            cache.setCrossReferences(CollectionHelper.merge(names, localIds));
            log.info("Finished Building allele -> crossReference map");
        }
    }

    private class GetConstructsThread implements Runnable {
        private String species;

        public GetConstructsThread(String species) {
            this.species = species;
        }

        @Override
        public void run() {
            log.info("Fetching allele -> constructs map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct) ";
            query += getSpeciesWhere(species);
            query += " RETURN allele.primaryKey as id, [construct.nameText, construct.primaryKey] as value";

            Map<String, Set<String>> constructs = getMapSetForQuery(query, getSpeciesParams(species));

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";
            query += getSpeciesWhere(species);
            query += "RETURN allele.primaryKey as id, synonym.name as value ";

            constructs = CollectionHelper.merge(constructs, getMapSetForQuery(query, getSpeciesParams(species)));

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:CROSS_REFERENCE]-(crossReference:CrossReference) ";
            query += getSpeciesWhere(species);
            query += "RETURN allele.primaryKey as id, crossReference.name as value ";

            constructs = CollectionHelper.merge(constructs, getMapSetForQuery(query, getSpeciesParams(species)));

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:ALSO_KNOWN_AS]-(secondaryId:SecondaryId) ";
            query += getSpeciesWhere(species);
            query += " RETURN allele.primaryKey as id, secondaryId.name as value";

            constructs = CollectionHelper.merge(constructs, getMapSetForQuery(query, getSpeciesParams(species)));

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:EXPRESSES|TARGETS|IS_REGULATED_BY]-(gene:Gene) ";
            query += getSpeciesWhere(species);
            query += " RETURN allele.primaryKey as id, gene.primaryKey as value; ";

            constructs = CollectionHelper.merge(constructs, getMapSetForQuery(query, getSpeciesParams(species)));
            
            cache.setConstructs(constructs);
            log.info("Finished Fetching allele -> constructs map");
        }
    }

    private class GetConstructExpressedComponentsThread implements Runnable {
        private String species;

        public GetConstructExpressedComponentsThread(String species) {
            this.species = species;
        }

        @Override
        public void run() {
            log.info("Fetching allele -> constructExpressedComponent map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:EXPRESSES]-(constructComponent:NonBGIConstructComponent) ";
            query += getSpeciesWhere(species);
            query += " RETURN allele.primaryKey as id, constructComponent.primaryKey as value";

            Map<String, Set<String>> result = getMapSetForQuery(query, getSpeciesParams(species));

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:EXPRESSES]-(gene:Gene) ";
            query += getSpeciesWhere(species);
            query += " RETURN allele.primaryKey as id, gene.symbolWithSpecies as value; ";
            
            cache.setConstructExpressedComponents(CollectionHelper.merge(result, getMapSetForQuery(query, getSpeciesParams(species))));
            log.info("Finished Fetching allele -> constructExpressedComponent map");
        }
    }

    private class GetConstructKnockdownComponent implements Runnable {
        private String species;

        public GetConstructKnockdownComponent(String species) {
            this.species = species;
        }

        @Override
        public void run() {
            log.info("Fetching allele -> constructKnockdownComponent map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:TARGETS]-(constructComponent:NonBGIConstructComponent) ";
            query += getSpeciesWhere(species);
            query += " RETURN allele.primaryKey as id, constructComponent.primaryKey as value";

            Map<String, Set<String>> result = getMapSetForQuery(query, getSpeciesParams(species));

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:TARGETS]-(gene:Gene) ";
            query += getSpeciesWhere(species);
            query += " RETURN allele.primaryKey as id, gene.symbolWithSpecies as value; ";

            cache.setConstructKnockdownComponents(CollectionHelper.merge(result, getMapSetForQuery(query, getSpeciesParams(species))));
            log.info("Finished Fetching allele -> constructKnockdownComponent map");
        }
    }
    
    private class GetConstructRegulatoryRegions implements Runnable {
        private String species;

        public GetConstructRegulatoryRegions(String species) {
            this.species = species;
        }

        @Override
        public void run() {
            log.info("Fetching allele -> constructRegulatoryRegion map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:IS_REGULATED_BY]-(constructComponent:NonBGIConstructComponent) ";
            query += getSpeciesWhere(species);
            query += " RETURN allele.primaryKey as id, constructComponent.primaryKey as value";

            Map<String, Set<String>> result = getMapSetForQuery(query, getSpeciesParams(species));

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:IS_REGULATED_BY]-(gene:Gene) ";
            query += getSpeciesWhere(species);
            query += " RETURN allele.primaryKey as id, gene.symbolWithSpecies as value; ";

            cache.setConstructRegulatoryRegions(CollectionHelper.merge(result, getMapSetForQuery(query, getSpeciesParams(species))));
            log.info("Finished Fetching allele -> constructRegulatoryRegion map");
        }
    }

    private class GetDiseaseMapThread implements Runnable {
        private String species;

        public GetDiseaseMapThread(String species) {
            this.species = species;
        }

        @Override
        public void run() {
            log.info("Building allele -> diseases map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:IS_IMPLICATED_IN]-(disease:DOTerm) ";
            query += getSpeciesWhere(species);
            query += " RETURN a.primaryKey, disease.nameKey ";

            cache.setDiseases(getMapSetForQuery(query, "a.primaryKey", "disease.nameKey", getSpeciesParams(species)));
            log.info("Finished Building allele -> diseases map");
        }
    }

    private class GetDiseasesAgrSlimMapThread implements Runnable {
        private String species;

        public GetDiseasesAgrSlimMapThread(String species) {
            this.species = species;
        }

        @Override
        public void run() {
            log.info("Building allele -> diseasesAgrSlim map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:IS_IMPLICATED_IN]-(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm)";
            query += " WHERE disease.subset =~ '.*DO_AGR_slim.*' ";
            if (StringUtils.isNotEmpty(species)) {
                query += " AND species.name = {species} ";
            }
            query += " RETURN a.primaryKey, disease.nameKey ";

            Map<String,String> params = new HashMap<String,String>();
            if (StringUtils.isNotEmpty(species)) {
                params.put("species",species);
            }

            cache.setDiseasesAgrSlim(getMapSetForQuery(query, "a.primaryKey", "disease.nameKey", params));
            log.info("Finished Building allele -> diseasesAgrSlim map");
        }
    }
    
    private class GetDiseaseWithParentsThread implements Runnable {
        private String species;

        public GetDiseaseWithParentsThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Building allele -> diseasesWithParents map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:IS_IMPLICATED_IN]-(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm)";
            query += getSpeciesWhere(species);
            query += " RETURN a.primaryKey, disease.nameKey ";

            cache.setDiseasesWithParents(getMapSetForQuery(query, "a.primaryKey", "disease.nameKey", getSpeciesParams(species)));
            log.info("Finished Building allele -> diseasesWithParents map");
        }
    }

    private class GetGenesMapThread implements Runnable {
        private String species;

        public GetGenesMapThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Building allele -> genes map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:IS_ALLELE_OF]-(a:Allele) ";
            query += getSpeciesWhere(species);
            query += "RETURN distinct a.primaryKey, gene.symbolWithSpecies";

            cache.setGenes(getMapSetForQuery(query, "a.primaryKey", "gene.symbolWithSpecies", getSpeciesParams(species)));
            log.info("Finished Building allele -> genes map");
        }
    }
    
    private class GetModelsThread implements Runnable {
        private String species;

        public GetModelsThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Building allele -> model map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:MODEL_COMPONENT]-(allele:Allele)";
            query += getSpeciesWhere(species);
            query += " RETURN allele.primaryKey as id, model.nameTextWithSpecies as value";

            cache.setModels(getMapSetForQuery(query, getSpeciesParams(species)));
            log.info("Finished Building allele -> model map");
        }
    }
    
    private class GetPhenotypeStatementsMapThread implements Runnable {
        private String species;

        public GetPhenotypeStatementsMapThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Building allele -> phenotype statements map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:HAS_PHENOTYPE]-(phenotype:Phenotype) ";
            query += getSpeciesWhere(species);
            query += " RETURN distinct a.primaryKey, phenotype.phenotypeStatement ";

            cache.setPhenotypeStatements(getMapSetForQuery(query, "a.primaryKey", "phenotype.phenotypeStatement", getSpeciesParams(species)));
            log.info("Finished Building allele -> phenotype statements map");
        }
    }
    
    private class GetVariantsThread implements Runnable {
        private String species;

        public GetVariantsThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Building allele -> variant name map");
            String query = " MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant) ";
            query += getSpeciesWhere(species);
            query += " RETURN distinct a.primaryKey as id, v.name as value";

            cache.setVariants(getMapSetForQuery(query, getSpeciesParams(species)));
            log.info("Finished Building allele -> variant name map");
        }
    }
    
    private class GetVariantSynonymsThread implements Runnable {
        private String species;

        public GetVariantSynonymsThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Building allele -> variant synonyms map");
            String query = " MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:ASSOCIATION]-(tlc:TranscriptLevelConsequence)  ";
            query += getSpeciesWhere(species);
            query += "RETURN distinct a.primaryKey as id, [v.hgvsNomenclature, tlc.hgvsVEPGeneNomenclature, tlc.hgvsProteinNomenclature, tlc.hgvsCodingNomenclature] as value  ";
            Map<String,Set<String>> tlcNames = getMapSetForQuery(query, getSpeciesParams(species));

            query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";
            query += getSpeciesWhere(species);
            query += " RETURN a.primaryKey as id, synonym.name as value ";
            Map<String,Set<String>> synonyms = getMapSetForQuery(query, getSpeciesParams(species));

            cache.setVariantSynonyms(CollectionHelper.merge(tlcNames, synonyms));
            log.info("Finished Building allele -> variant synonyms map");
        }
    }
    
    private class GetVariantTypeMapThread implements Runnable {
        private String species;

        public GetVariantTypeMapThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Building allele -> variant types map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:VARIATION_TYPE]-(term:SOTerm) ";
            query += getSpeciesWhere(species);
            query += " RETURN distinct a.primaryKey,term.name ";

            cache.setVariantType(getMapSetForQuery(query, "a.primaryKey", "term.name", getSpeciesParams(species)));
            log.info("Finished Building allele -> variant types map");
        }
    }
    
    private class GetMolecularConsequence implements Runnable {
        private String species;

        public GetMolecularConsequence(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Building allele -> molecular consequence map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:ASSOCIATION]-(consequence:GeneLevelConsequence) ";
            query += getSpeciesWhere(species);
            query += " RETURN a.primaryKey as id, consequence.geneLevelConsequence as value ";

            cache.setMolecularConsequenceMap(getMapSetForQuery(query, getSpeciesParams(species)));
            log.info("Finished Building allele -> molecular consequence map");
        }
    }

}
