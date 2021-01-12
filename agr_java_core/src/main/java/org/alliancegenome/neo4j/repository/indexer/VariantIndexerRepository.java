package org.alliancegenome.neo4j.repository.indexer;

import java.util.*;
import java.util.concurrent.*;

import org.alliancegenome.es.index.site.cache.IndexerCache;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.repository.Neo4jRepository;
import org.apache.logging.log4j.*;

public class VariantIndexerRepository extends Neo4jRepository<Variant> {

    private final Logger log = LogManager.getLogger(getClass());

    private IndexerCache cache = new IndexerCache();
    
    public VariantIndexerRepository() {  super(Variant.class); }
    
    public IndexerCache getVariantCache(String species) {
        log.info("Building VariantDocumentCache");

        ExecutorService executor = Executors.newFixedThreadPool(20); // Run all at once
        
        executor.execute(new GetVariantMapThread(species));
        executor.execute(new GetAlleleMapThread(species));
        executor.execute(new GetVariantTypeMapThread(species));
        executor.execute(new GetGeneMapThread(species));
        executor.execute(new GetSpeciesThread(species));
        executor.execute(new GetMolecularConsequenceThread(species));
        
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        log.info("Finished Building VariantDocumentCache");

        return cache;

    }
    
    private class GetVariantMapThread implements Runnable {
        private String species;

        public GetVariantMapThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Fetching variants");
            String query = "MATCH pVariant=(species:Species)-[:FROM_SPECIES]-(a:Allele)--(v:Variant) ";
            query += getSpeciesWhere(species);
            query += " RETURN v;";

            Iterable<Variant> variants = null;

            if (species != null) {
                variants = query(query, getSpeciesParams(species));
            } else {
                variants = query(query);
            }

            Map<String, Variant> variantMap = new HashMap<>();
            for (Variant v : variants) {
                variantMap.put(v.getPrimaryKey(), v);
            }
            cache.setVariantMap(variantMap);
            log.info("Finished Fetching variants");
        }
    }

    private class GetAlleleMapThread implements Runnable {
        private String species;

        public GetAlleleMapThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Fetching alleles");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:VARIATION]-(variant:Variant) ";
            query += getSpeciesWhere(species);
            query += " RETURN variant.primaryKey as id, allele.symbolTextWithSpecies as value";

            cache.setAlleles(getMapSetForQuery(query, getSpeciesParams(species)));
            log.info("Finished Fetching alleles");
        }
    }

    private class GetVariantTypeMapThread implements Runnable {
        private String species;

        public GetVariantTypeMapThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Fetching DNA Change Types");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:VARIATION_TYPE]-(term:SOTerm) ";
            query += getSpeciesWhere(species);
            query += " RETURN distinct v.primaryKey as id, term.name as value";

            cache.setVariantType(getMapSetForQuery(query, getSpeciesParams(species)));
            log.info("Finished Fetching DNA Change Types");
        }
    }

    private class GetGeneMapThread implements Runnable {
        private String species;

        public GetGeneMapThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Fetching genes");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:COMPUTED_GENE]-(variant:Variant) ";
            query += getSpeciesWhere(species);
            query += " RETURN variant.primaryKey as id, gene.symbolWithSpecies as value";

            cache.setGenes(getMapSetForQuery(query, getSpeciesParams(species)));
            log.info("Finished Fetching genes");
        }
    }

    private class GetSpeciesThread implements Runnable {
        private String species;

        public GetSpeciesThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Fetching species");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:VARIATION]-(variant:Variant) ";
            query += getSpeciesWhere(species);
            query += "RETURN variant.primaryKey as id, species.name as value";

            cache.setSpecies(getMapSetForQuery(query, getSpeciesParams(species)));
            log.info("Finished Fetching species");
        }
        
    }

    private class GetMolecularConsequenceThread implements Runnable {
        private String species;

        public GetMolecularConsequenceThread(String species) {
            this.species = species;
        }
        @Override
        public void run() {
            log.info("Fetching molecular consequences");
            String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:ASSOCIATION]-(consequence:GeneLevelConsequence) ";
            query += getSpeciesWhere(species);
            query += " RETURN v.primaryKey as id, consequence.geneLevelConsequence as value ";

            cache.setMolecularConsequenceMap(getMapSetForQuery(query, getSpeciesParams(species)));
            log.info("Finished Fetching molecular consequences");
        }
        
    }


}
