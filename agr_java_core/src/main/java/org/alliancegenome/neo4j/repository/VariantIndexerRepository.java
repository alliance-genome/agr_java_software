package org.alliancegenome.neo4j.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.cache.IndexerCache;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VariantIndexerRepository extends Neo4jRepository<Variant> {

    private final Logger log = LogManager.getLogger(getClass());

    public VariantIndexerRepository() {  super(Variant.class); }

    public Map<String, Variant> getVariantMap(String species) {
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
        return variantMap;
    }

    public IndexerCache getCache(String species) {
        IndexerCache cache = new IndexerCache();

        log.info("Fetching variants");
        cache.setVariantMap(getVariantMap(species));

        log.info("Fetching alleles");
        cache.setAlleles(getAlleleMap(species));

        log.info("Fetching DNA Change Types");
        cache.setVariantType(getVariantTypeMap(species));

        log.info("Fetching genes");
        cache.setGenes(getGeneMap(species));

        log.info("Fetching species");
        cache.setSpecies(getSpecies(species));

        log.info("Fetching molecular consequences");
        cache.setMolecularConsequenceMap(getMolecularConsequence(species));


        return cache;
    }

    public Map<String, Set<String>> getAlleleMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:VARIATION]-(variant:Variant) ";
        query += getSpeciesWhere(species);
        query += " RETURN variant.primaryKey as id, allele.symbolTextWithSpecies as value";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

    public Map<String, Set<String>> getVariantTypeMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:VARIATION_TYPE]-(term:SOTerm) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct v.primaryKey as id, term.name as value";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }


    public Map<String, Set<String>> getGeneMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)-[:COMPUTED_GENE]-(variant:Variant) ";
        query += getSpeciesWhere(species);
        query += " RETURN variant.primaryKey as id, gene.symbolWithSpecies as value";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

    public Map<String, Set<String>> getSpecies(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:VARIATION]-(variant:Variant) ";
        query += getSpeciesWhere(species);
        query += "RETURN variant.primaryKey as id, species.name as value";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

    public Map<String, Set<String>> getMolecularConsequence(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:ASSOCIATION]-(consequence:GeneLevelConsequence) ";
        query += getSpeciesWhere(species);
        query += " RETURN v.primaryKey as id, consequence.geneLevelConsequence as value ";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

}
