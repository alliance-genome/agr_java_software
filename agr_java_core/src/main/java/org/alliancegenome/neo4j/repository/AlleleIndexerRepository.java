package org.alliancegenome.neo4j.repository;

import org.alliancegenome.es.index.site.cache.AlleleDocumentCache;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AlleleIndexerRepository extends Neo4jRepository {

    private final Logger log = LogManager.getLogger(getClass());

    public AlleleIndexerRepository() { super(Allele.class); }

    public Map<String, Allele> getAlleleMap(String species) {

        String query = "MATCH p1=(feature:Feature)-[:IS_ALLELE_OF]-(:Gene)-[:FROM_SPECIES]-(species:Species) ";
        query += getSpeciesWhere(species);
        query += " OPTIONAL MATCH pSyn=(feature:Feature)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";
        query += " OPTIONAL MATCH crossRef=(feature:Feature)-[:CROSS_REFERENCE]-(c:CrossReference) ";
        query += " RETURN p1, pSyn, crossRef ";

        Iterable<Allele> alleles = null;

        if (species != null) {
            alleles = query(query, getSpeciesParams(species));
        } else {
            alleles = query(query);
        }

        Map<String,Allele> alleleMap = new HashMap<>();
        for (Allele allele : alleles) {
            alleleMap.put(allele.getPrimaryKey(),allele);
        }
        return alleleMap;
    }

    public AlleleDocumentCache getAlleleDocumentCache(String species) {
        AlleleDocumentCache alleleDocumentCache = new AlleleDocumentCache();

        log.info("Fetching alleles");
        alleleDocumentCache.setAlleleMap(getAlleleMap(species));

        log.info("Building allele -> diseases map");
        alleleDocumentCache.setDiseases(getDiseaseMap(species));
        log.info("Building allele -> genes map");
        alleleDocumentCache.setGenes(getGenesMap(species));
        log.info("Building allele -> phenotype statements map");
        alleleDocumentCache.setPhenotypeStatements(getPhenotypeStatementsMap(species));

        return alleleDocumentCache;

    }

    public Map<String, Set<String>> getDiseaseMap(String species) {
        String query = "MATCH (species:Species)--(:Gene)-[:IS_ALLELE_OF]-(a:Allele)--(disease:DOTerm) ";
        query += getSpeciesWhere(species);
        query += " RETURN a.primaryKey, disease.nameKey ";

        return getMapSetForQuery(query, "feature.primaryKey", "disease.nameKey", getSpeciesParams(species));
    }

    public Map<String, Set<String>> getGenesMap(String species) {
        //todo: needs to be nameKey, but nameKey needs to be set in neo
        String query = "MATCH (species:Species)--(gene:Gene)-[:IS_ALLELE_OF]-(a:Allele) ";
        query += getSpeciesWhere(species);
        query += "RETURN distinct a.primaryKey, gene.symbol";

        return getMapSetForQuery(query, "a.primaryKey", "gene.symbol", getSpeciesParams(species));
    }


    public Map<String, Set<String>> getPhenotypeStatementsMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[:IS_ALLELE_OF]-(a:Allele)--(phenotype:Phenotype) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct a.primaryKey, phenotype.phenotypeStatement ";

        return getMapSetForQuery(query, "a.primaryKey", "phenotype.phenotypeStatement", getSpeciesParams(species));
    }
}
