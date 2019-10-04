package org.alliancegenome.neo4j.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.cache.DiseaseDocumentCache;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiseaseIndexerRepository extends Neo4jRepository<DOTerm> {

    private Logger log = LogManager.getLogger(getClass());

    public DiseaseIndexerRepository() {
        super(DOTerm.class);
    }

    //todo: maps for gene, species, diseaseGroup, parentNames

    public Map<String,DOTerm> getDiseaseMap() {
        String query = "MATCH pDisease=(disease:DOTerm) WHERE disease.isObsolete = 'false' ";
        query += " OPTIONAL MATCH pSyn=(disease:DOTerm)-[:ALSO_KNOWN_AS]-(:Synonym) ";
        query += " OPTIONAL MATCH pCR=(disease:DOTerm)-[:CROSS_REFERENCE]-(:CrossReference)";
        query += " OPTIONAL MATCH pSecondaryId=(disease:DOTerm)-[:ALSO_KNOWN_AS]-(s:SecondaryId)";
        query += " RETURN pDisease, pSyn, pCR, pSecondaryId";

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

        log.info("Building disease -> gene map");
        diseaseDocumentCache.setGenes(getGenesMap());

        log.info("Building disease -> allele map");
        diseaseDocumentCache.setAlleles(getAllelesMap());

        log.info("Building disease -> model map");
        diseaseDocumentCache.setModels(getModelsMap());

        log.info("Building disease -> species map");
        diseaseDocumentCache.setSpeciesMap(getSpeciesMap());

        log.info("Building disease -> diseaseGroup map");
        diseaseDocumentCache.setDiseaseGroupMap(getDiseaseGroupMap());

        log.info("Building disease -> disease parent name map");
        diseaseDocumentCache.setParentNameMap(getParentNameMap());

        return diseaseDocumentCache;
    }


    public Map<String, Set<String>> getGenesMap() {
        return getMapSetForQuery("MATCH (disease:DOTerm)--(:DiseaseEntityJoin)--(gene:Gene) " +
                " RETURN disease.primaryKey as id, gene.symbolWithSpecies as value;");
    }

    public Map<String, Set<String>> getAllelesMap() {
        return getMapSetForQuery("MATCH (disease:DOTerm)--(:DiseaseEntityJoin)--(allele:Allele) " +
                "RETURN disease.primaryKey as id, allele.symbolTextWithSpecies as value;");
    }

    public Map<String, Set<String>> getModelsMap() {
        return getMapSetForQuery("MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)--(disease:DOTerm)"
                + " RETURN disease.primaryKey as id, model.nameTextWithSpecies as value");
    }

    public Map<String, Set<String>> getSpeciesMap() {
        return getMapSetForQuery("MATCH (disease:DOTerm)--(:DiseaseEntityJoin)--(gene:Gene)--(species:Species) " +
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
