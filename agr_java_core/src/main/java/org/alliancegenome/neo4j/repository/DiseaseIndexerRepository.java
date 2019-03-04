package org.alliancegenome.neo4j.repository;

import org.alliancegenome.es.index.site.cache.DiseaseDocumentCache;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.model.Result;

import java.util.*;

public class DiseaseIndexerRepository extends Neo4jRepository<DOTerm> {

    private Logger log = LogManager.getLogger(getClass());

    public DiseaseIndexerRepository() {
        super(DOTerm.class);
    }

    //todo: maps for gene, species, diseaseGroup, parentNames

    public Map<String,DOTerm> getDiseaseMap() {
        String query = "MATCH pDisease=(disease:DOTerm) ";
        query += " OPTIONAL MATCH pSyn=(disease:DOTerm)-[:ALSO_KNOWN_AS]-(:Synonym) ";
        query += " OPTIONAL MATCH pCR=(disease:DOTerm)-[:CROSS_REFERENCE]-(:CrossReference)";
        query += " RETURN pDisease, pSyn, pCR";

        Iterable<DOTerm> diseases = query(query);

        Map<String,DOTerm> diseaseMap = new HashMap<>();
        for (DOTerm disease : diseases) {
            diseaseMap.put(disease.getPrimaryKey(), disease);
        }
        return diseaseMap;
    }

    public DiseaseDocumentCache getDiseaseDocumentCache() {
        DiseaseDocumentCache diseaseDocumentCache = new DiseaseDocumentCache();

        log.info("Fetching diseases");
        diseaseDocumentCache.setDiseaseMap(getDiseaseMap());

        log.info("Building disease -> gene nameJey map");
        diseaseDocumentCache.setGenesMap(getGenesMap());

        log.info("BUilding disease -> allele map");
        diseaseDocumentCache.setAllelesMap(getAllelesMap());

        log.info("Building disease -> species map");
        diseaseDocumentCache.setSpeciesMap(getSpeciesMap());

        log.info("Building disease -> diseaseGroup map");
        diseaseDocumentCache.setDiseaseGroupMap(getDiseaseGroupMap());

        log.info("Building disease -> disease parent name map");
        diseaseDocumentCache.setParentNameMap(getParentNameMap());

        return diseaseDocumentCache;
    }


    public Map<String, Set<String>> getGenesMap() {
        return getMapSetForQuery("MATCH (disease:DOTerm)-[:IS_IMPLICATED_IN]-(gene:Gene) " +
                " RETURN disease.primaryKey as id, gene.symbolWithSpecies as value;");
    }

    public Map<String, Set<String>> getAllelesMap() {
        return getMapSetForQuery("MATCH (disease:DOTerm)--(gene:Gene)-[:IS_ALLELE_OF]-(allele:Allele) " +
                "RETURN disease.primaryKey as id, allele.symbolText as value;");
    }

    public Map<String, Set<String>> getSpeciesMap() {
        return getMapSetForQuery("MATCH (disease:DOTerm)-[:IS_IMPLICATED_IN]-(gene:Gene)-[:FROM_SPECIES]-(species:Species) " +
                " RETURN disease.primaryKey as id, species.name as value;");
    }

    public Map<String, Set<String>> getParentNameMap() {
        return getMapSetForQuery("MATCH (disease:DOTerm)-[:IS_A_PART_OF_CLOSURE]-(parent:DOTerm) " +
                "RETURN disease.primaryKey as id, disease.nameKey, parent.nameKey as value;");
    }

    public Map<String, Set<String>> getDiseaseGroupMap() {
        return getMapSetForQuery("MATCH (disease:DOTerm)-[:IS_A_PART_OF_CLOSURE]-(parent:DOTerm) " +
                "WHERE parent.subset =~ '.*DO_AGR_slim.*' " +
                "RETURN disease.primaryKey as id, parent.nameKey as value;");
    }

}
