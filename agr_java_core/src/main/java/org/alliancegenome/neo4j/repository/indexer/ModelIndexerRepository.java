package org.alliancegenome.neo4j.repository.indexer;

import java.util.*;
import java.util.concurrent.*;

import org.alliancegenome.es.index.site.cache.ModelDocumentCache;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.alliancegenome.neo4j.repository.Neo4jRepository;
import org.apache.logging.log4j.*;

public class ModelIndexerRepository extends Neo4jRepository {

    private final Logger log = LogManager.getLogger(getClass());

    //indexing doesn't need the class defined, but once it is, it can go here
    public ModelIndexerRepository() { super(AffectedGenomicModel.class); }

    private ModelDocumentCache cache = new ModelDocumentCache();

    public ModelDocumentCache getModelDocumentCache() {
        log.info("Building ModelDocumentCache");

        ExecutorService executor = Executors.newFixedThreadPool(20); // Run all at once
        
        executor.execute(new GetModelMapThread());
        executor.execute(new GetAlleleMapThread());
        executor.execute(new GetDiseaseMapThread());
        executor.execute(new GetDiseaeAgrSlimMapThread());
        executor.execute(new GetDiseasesWithParents());
        executor.execute(new GetGeneMapThread());
        executor.execute(new GetPhenotypeStatementsThread());
        
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
        @Override
        public void run() {
            log.info("Fetching models");
            String query = "MATCH pModel = (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel) ";
            query += "OPTIONAL MATCH pSyn=(model:AffectedGenomicModel)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";
            query += "RETURN pModel, pSyn";

            Iterable<AffectedGenomicModel> models = null;

            models = query(query);

            Map<String, AffectedGenomicModel> modelMap = new HashMap<>();
            for (AffectedGenomicModel model : models) {
                modelMap.put(model.getPrimaryKey(), model);
            }
            cache.setModelMap(modelMap);
            log.info("Finished Fetching models");
        }
    }

    private class GetAlleleMapThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching model -> allele map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:MODEL_COMPONENT]-(allele:Allele)";
            query += " RETURN model.primaryKey as id, allele.symbolTextWithSpecies as value";
            cache.setAlleles(getMapSetForQuery(query));
            log.info("Finished Fetching model -> allele map");
        }
    }

    private class GetDiseaseMapThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching model -> disease map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:IS_MODEL_OF]-(disease:DOTerm)";
            query += " RETURN model.primaryKey as id, disease.name as value";

            cache.setDiseases(getMapSetForQuery(query));
            log.info("Finished Fetching model -> disease map");
        }
    }
    
    private class GetDiseaeAgrSlimMapThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching model -> disease slim map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:IS_MODEL_OF]-(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm) ";
            query += " WHERE disease.subset =~ '.*DO_AGR_slim.*' ";
            query += " RETURN distinct model.primaryKey as id, disease.nameKey as value ";

            cache.setDiseasesAgrSlim(getMapSetForQuery(query));
            log.info("Finished Fetching model -> disease slim map");
        }
        
    }
    
    private class GetDiseasesWithParents implements Runnable {

        @Override
        public void run() {
            log.info("Fetching model -> disease with parents map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:IS_MODEL_OF]-(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm) ";
            query += " RETURN distinct model.primaryKey as id, disease.nameKey as value ";

            cache.setDiseasesWithParents(getMapSetForQuery(query));
            log.info("Finished Fetching model -> disease with parents map");
        }
        
    }

    private class GetGeneMapThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching model -> gene map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:MODEL_COMPONENT|:SEQUENCE_TARGETING_REAGENT]-(feature)--(gene:Gene)";
            query += " RETURN model.primaryKey as id, gene.symbolWithSpecies as value";

            cache.setGenes(getMapSetForQuery(query));
            log.info("Finished Fetching model -> gene map");
        }
        
    }

    private class GetPhenotypeStatementsThread implements Runnable {
        @Override
        public void run() {
            log.info("Fetching model -> phenotypeStatement map");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:HAS_PHENOTYPE]-(phenotype:Phenotype) ";
            query += " RETURN model.primaryKey as id, phenotype.phenotypeStatement as value ";

            cache.setPhenotypeStatements(getMapSetForQuery(query));
            log.info("Finished Fetching model -> phenotypeStatement map");
        }
    }

}
