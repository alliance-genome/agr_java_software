package org.alliancegenome.neo4j.repository.indexer;

import java.util.*;
import java.util.concurrent.*;

import org.alliancegenome.es.index.site.cache.ModelDocumentCache;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.alliancegenome.neo4j.repository.Neo4jRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.*;

public class ModelIndexerRepository extends Neo4jRepository {

    private final Logger log = LogManager.getLogger(getClass());

    //indexing doesn't need the class defined, but once it is, it can go here
    public ModelIndexerRepository() { super(AffectedGenomicModel.class); }

    private ModelDocumentCache cache = new ModelDocumentCache();

    public ModelDocumentCache getModelDocumentCache(String species) {
        log.info("Building ModelDocumentCache");

        ExecutorService executor = Executors.newFixedThreadPool(20); // Run all at once
        
        executor.execute(new GetModelMapThread(species));
        executor.execute(new GetAlleleMapThread(species));
        executor.execute(new GetDiseaseMapThread(species));
        executor.execute(new GetDiseaeAgrSlimMapThread(species));
        executor.execute(new GetDiseasesWithParents(species));
        executor.execute(new GetGeneMapThread(species));
        executor.execute(new GetPhenotypeStatementsThread(species));
        
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("Finished Building ModelDocumentCache");

        return cache;
    }

    private class GetModelMapThread implements Runnable {
        private String species;

        public GetModelMapThread(String species) {
            this.species = species;
        }

        @Override
        public void run() {
            log.info("Fetching models");
            String query = "MATCH pModel = (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel) ";
            query += getSpeciesWhere(species);
            query += "OPTIONAL MATCH pSyn=(model:AffectedGenomicModel)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";
            query += "RETURN pModel, pSyn";

            Iterable<AffectedGenomicModel> models = null;

            if (species != null) {
                models = query(query, getSpeciesParams(species));
            } else {
                models = query(query);
            }

            Map<String, AffectedGenomicModel> modelMap = new HashMap<>();
            for (AffectedGenomicModel model : models) {
                modelMap.put(model.getPrimaryKey(), model);
            }
            cache.setModelMap(modelMap);
            log.info("Finished Fetching models");
        }
    }

    private class GetAlleleMapThread implements Runnable {

        private String species;

        public GetAlleleMapThread(String species) {
            this.species = species;
        }

        @Override
        public void run() {
            log.info("Fetching model -> allele map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:MODEL_COMPONENT]-(allele:Allele)";
            query += getSpeciesWhere(species);
            query += " RETURN model.primaryKey as id, allele.symbolTextWithSpecies as value";
            cache.setAlleles(getMapSetForQuery(query, getSpeciesParams(species)));
            log.info("Finished Fetching model -> allele map");
        }
    }

    private class GetDiseaseMapThread implements Runnable {
        
        private String species;

        public GetDiseaseMapThread(String species) {
            this.species = species;
        }
        
        @Override
        public void run() {
            log.info("Fetching model -> disease map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:IS_MODEL_OF]-(disease:DOTerm)";
            query += getSpeciesWhere(species);
            query += " RETURN model.primaryKey as id, disease.name as value";

            cache.setDiseases(getMapSetForQuery(query, getSpeciesParams(species)));
            log.info("Finished Fetching model -> disease map");
        }
    }
    
    private class GetDiseaeAgrSlimMapThread implements Runnable {
        
        private String species;

        public GetDiseaeAgrSlimMapThread(String species) {
            this.species = species;
        }
        
        @Override
        public void run() {
            log.info("Fetching model -> disease slim map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:IS_MODEL_OF]-(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm) ";
            query += " WHERE disease.subset =~ '.*DO_AGR_slim.*' ";

            Map<String,String> params = new HashMap<String,String>();

            if (StringUtils.isNotEmpty(species)) {
                query += " AND species.name = {species} ";
                params.put("species", species);
            }
            query += " RETURN distinct model.primaryKey as id, disease.nameKey as value ";

            cache.setDiseasesAgrSlim(getMapSetForQuery(query, params));
            log.info("Finished Fetching model -> disease slim map");
        }
        
    }
    
    private class GetDiseasesWithParents implements Runnable {
        private String species;

        public GetDiseasesWithParents(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Fetching model -> disease with parents map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:IS_MODEL_OF]-(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm) ";
            query += getSpeciesWhere(species);
            query += " RETURN distinct model.primaryKey as id, disease.nameKey as value ";

            cache.setDiseasesWithParents(getMapSetForQuery(query, getSpeciesParams(species)));
            log.info("Finished Fetching model -> disease with parents map");
        }
        
    }

    private class GetGeneMapThread implements Runnable {
        private String species;

        public GetGeneMapThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Fetching model -> gene map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:MODEL_COMPONENT|:SEQUENCE_TARGETING_REAGENT]-(feature)--(gene:Gene)";
            query += getSpeciesWhere(species);
            query += " RETURN model.primaryKey as id, gene.symbolWithSpecies as value";

            cache.setGenes(getMapSetForQuery(query, getSpeciesParams(species)));
            log.info("Finished Fetching model -> gene map");
        }
        
    }

    private class GetPhenotypeStatementsThread implements Runnable {
        private String species;

        public GetPhenotypeStatementsThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Fetching model -> phenotypeStatement map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:HAS_PHENOTYPE]-(phenotype:Phenotype) ";
            query += getSpeciesWhere(species);
            query += " RETURN model.primaryKey as id, phenotype.phenotypeStatement as value ";

            cache.setPhenotypeStatements(getMapSetForQuery(query, getSpeciesParams(species)));
            log.info("Finished Fetching model -> phenotypeStatement map");
        }
    }

}
