package org.alliancegenome.neo4j.repository;

import org.alliancegenome.es.index.site.cache.FeatureDocumentCache;
import org.alliancegenome.neo4j.entity.node.Feature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FeatureIndexerRepository extends Neo4jRepository {

    private final Logger log = LogManager.getLogger(getClass());

    public FeatureIndexerRepository() { super(Feature.class); }

    public Map<String, Feature> getFeatureMap(String species) {

        String query = "MATCH p1=(feature:Feature)-[:IS_ALLELE_OF]-(:Gene)-[:FROM_SPECIES]-(species:Species) ";
        query += getSpeciesWhere(species);
        query += " OPTIONAL MATCH pSyn=(feature:Feature)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";
        query += " OPTIONAL MATCH crossRef=(feature:Feature)-[:CROSS_REFERENCE]-(c:CrossReference) ";
        query += " RETURN p1, pSyn, crossRef ";

        Iterable<Feature> features = null;

        if (species != null) {
            features = query(query, getSpeciesParams(species));
        } else {
            features = query(query);
        }

        Map<String,Feature> featureMap = new HashMap<>();
        for (Feature feature : features) {
            featureMap.put(feature.getPrimaryKey(),feature);
        }
        return featureMap;
    }

    public FeatureDocumentCache getFeatureDocumentCache(String species) {
        FeatureDocumentCache featureDocumentCache = new FeatureDocumentCache();

        log.info("Fetching features");
        featureDocumentCache.setFeatureMap(getFeatureMap(species));

        log.info("Building feature -> diseases map");
        featureDocumentCache.setDiseases(getDiseaseMap(species));
        log.info("Building feature -> genes map");
        featureDocumentCache.setGenes(getGenesMap(species));
        log.info("Building feature -> phenotype statements map");
        featureDocumentCache.setPhenotypeStatements(getPhenotypeStatementsMap(species));

        return featureDocumentCache;

    }

    public Map<String, Set<String>> getDiseaseMap(String species) {
        String query = "MATCH (species:Species)--(:Gene)-[:IS_ALLELE_OF]-(feature:Feature)--(disease:DOTerm) ";
        query += getSpeciesWhere(species);
        query += " RETURN feature.primaryKey, disease.nameKey ";

        return getMapSetForQuery(query, "feature.primaryKey", "disease.nameKey", getSpeciesParams(species));
    }

    public Map<String, Set<String>> getGenesMap(String species) {
        //todo: needs to be nameKey, but nameKey needs to be set in neo
        String query = "MATCH (species:Species)--(gene:Gene)-[:IS_ALLELE_OF]-(feature:Feature) ";
        query += getSpeciesWhere(species);
        query += "RETURN distinct feature.primaryKey, gene.symbol";

        return getMapSetForQuery(query, "feature.primaryKey", "gene.symbol", getSpeciesParams(species));
    }


    public Map<String, Set<String>> getPhenotypeStatementsMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[:IS_ALLELE_OF]-(feature:Feature)--(phenotype:Phenotype) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct feature.primaryKey, phenotype.phenotypeStatement ";

        return getMapSetForQuery(query, "feature.primaryKey", "phenotype.phenotypeStatement", getSpeciesParams(species));
    }
}
