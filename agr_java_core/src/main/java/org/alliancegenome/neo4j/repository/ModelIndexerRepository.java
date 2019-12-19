package org.alliancegenome.neo4j.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.cache.ModelDocumentCache;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModelIndexerRepository extends Neo4jRepository {

    private final Logger log = LogManager.getLogger(getClass());

    //indexing doesn't need the class defined, but once it is, it can go here
    public ModelIndexerRepository() { super(AffectedGenomicModel.class); }

    public Map<String, AffectedGenomicModel> getModelMap(String species) {
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
        return modelMap;
    }

    public ModelDocumentCache getModelDocumentCache(String species) {
       ModelDocumentCache cache = new ModelDocumentCache();

       log.info("Fetching models");
       cache.setModelMap(getModelMap(species));

       log.info("Fetching model -> allele map");
       cache.setAlleles(getAlleleMap(species));

       log.info("Fetching model -> disease map");
       cache.setDiseases(getDiseaseMap(species));

       log.info("Fetching model -> disease slim map");
       cache.setDiseasesAgrSlim(getDiseasesAgrSlimMap(species));

       log.info("Fetching model -> disease with parents map");
       cache.setDiseasesWithParents(getDiseasesWithParents(species));

       log.info("Fetching model -> gene map");
       cache.setGenes(getGeneMap(species));

       log.info("Fetching model -> phenotypeStatement map");
       cache.setPhenotypeStatements(getPhenotypeStatementMap(species));

       return cache;
    }

    public Map<String, Set<String>> getAlleleMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)--(allele:Allele)";
        query += getSpeciesWhere(species);
        query += " RETURN model.primaryKey as id, allele.symbolTextWithSpecies as value";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

    public Map<String, Set<String>> getDiseaseMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)--(disease:DOTerm)";
        query += getSpeciesWhere(species);
        query += " RETURN model.primaryKey as id, disease.name as value";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

    private Map<String, Set<String>> getDiseasesAgrSlimMap(String species) {
        String query = "MATCH (species:Species)--(model:AffectedGenomicModel)--(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm) ";
        query += " WHERE disease.subset =~ '.*DO_AGR_slim.*' ";

        Map<String,String> params = new HashMap<String,String>();

        if (StringUtils.isNotEmpty(species)) {
            query += " AND species.name = {species} ";
            params.put("species",species);
        }
        query += " RETURN distinct model.primaryKey as id, disease.nameKey as value ";

        return getMapSetForQuery(query, params);
    }

    private Map<String, Set<String>> getDiseasesWithParents(String species) {
        String query = "MATCH (species:Species)--(model:AffectedGenomicModel)--(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct model.primaryKey as id, disease.nameKey as value ";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

    public Map<String, Set<String>> getGeneMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:MODEL_COMPONENT|:SEQUENCE_TARGETING_REAGENT]-(feature)--(gene:Gene)";
        query += getSpeciesWhere(species);
        query += " RETURN model.primaryKey as id, gene.symbolWithSpecies as value";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

    public Map<String, Set<String>> getPhenotypeStatementMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)--(phenotype:Phenotype) ";
        query += getSpeciesWhere(species);
        query += " RETURN model.primaryKey as id, phenotype.phenotypeStatement as value ";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

}
