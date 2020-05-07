package org.alliancegenome.neo4j.repository;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneIndexerRepository extends Neo4jRepository<Gene>  {

    private final Logger log = LogManager.getLogger(getClass());
    protected Runtime runtime = Runtime.getRuntime();
    protected DecimalFormat df = new DecimalFormat("#");
    
    public GeneIndexerRepository() {
        super(Gene.class);
    }

    public Map<String,Gene> getGeneMap(String species) {

        String query = " MATCH p1=(species:Species)-[:FROM_SPECIES]-(g:Gene) ";
        query += getSpeciesWhere(species);
        query += " OPTIONAL MATCH pSoTerm=(g:Gene)-[:ANNOTATED_TO]-(soTerm:SOTerm)";
        query += " RETURN p1, pSoTerm";

        Iterable<Gene> genes = null;

        if (species != null) {
            genes = query(query, getSpeciesParams(species));
        } else {
            genes = query(query);
        }

        Map<String,Gene> geneMap = new HashMap<>();
        for (Gene gene : genes) {
            geneMap.put(gene.getPrimaryKey(), gene);
        }

        return geneMap;

    }

    public GeneDocumentCache getGeneDocumentCache() {
        return getGeneDocumentCache(null);
    }

    public GeneDocumentCache getGeneDocumentCache(String species) {
        GeneDocumentCache geneDocumentCache = new GeneDocumentCache();

        checkMemory();
        log.info("Fetching genes");
        geneDocumentCache.setGeneMap(getGeneMap(species));

        checkMemory();
        log.info("Building gene -> synonyms map");
        geneDocumentCache.setSynonyms(getSynonyms(species));

        checkMemory();
        log.info("Building gene -> cross references map");
        geneDocumentCache.setCrossReferences(getCrossReferences(species));

        checkMemory();
        log.info("Building gene -> chromosome map");
        geneDocumentCache.setChromosomes(getChromosomes(species));

        checkMemory();
        log.info("Building gene -> secondaryId map");
        geneDocumentCache.setSecondaryIds(getSecondaryIds(species));

        checkMemory();
        log.info("Building gene -> alleles map");
        geneDocumentCache.setAlleles(getAllelesMap(species));

        checkMemory();
        log.info("Building gene -> strictOrthologySymbols map");
        geneDocumentCache.setStrictOrthologySymbols(getStrictOrthologySymbolsMap(species));

        checkMemory();
        log.info("Building gene -> soTermNameWithParents map");
        geneDocumentCache.setSoTermNameWithParents(getSoTermNameWithParentsMap(species));

        checkMemory();
        log.info("Building gene -> soTermNameAgrSlim map");
        geneDocumentCache.setSoTermNameAgrSlim(getSoTermNameAgrSlimMap(species));

        checkMemory();
        log.info("Building gene -> diseases map");
        geneDocumentCache.setDiseases(getDiseasesMap(species));

        checkMemory();
        log.info("Building gene -> diseasesAgrSlim map");
        geneDocumentCache.setDiseasesAgrSlim(getDiseasesAgrSlimMap(species));

        checkMemory();
        log.info("Building gene -> diseasesWithParents map");
        geneDocumentCache.setDiseasesWithParents(getDiseasesWithParents(species));

        checkMemory();
        log.info("Building gene -> model map");
        geneDocumentCache.setModels(getModelMap(species));

        checkMemory();
        log.info("Building gene -> phenotypeStatement map");
        geneDocumentCache.setPhenotypeStatements(getPhenotypeStatementMap(species));

        checkMemory();
        log.info("Building gene -> GO BP Slim map");
        geneDocumentCache.setBiologicalProcessAgrSlim(getGOTermMap("biological_process", true, species));
        
        checkMemory();
        log.info("Building gene -> GO CC Slim map");
        geneDocumentCache.setCellularComponentAgrSlim(getGOTermMap("cellular_component", true, species));
        
        checkMemory();
        log.info("Building gene -> GO MF Slim map");
        geneDocumentCache.setMolecularFunctionAgrSlim(getGOTermMap("molecular_function", true, species));

        checkMemory();
        log.info("Building gene -> GO BP w/parents map");
        geneDocumentCache.setBiologicalProcessWithParents(getGOTermMap("biological_process", false, species));
        
        checkMemory();
        log.info("Building gene -> GO CC w/parents map");
        geneDocumentCache.setCellularComponentWithParents(getGOTermMap("cellular_component", false, species));
        
        checkMemory();
        log.info("Building gene -> GO MF w/parents map");
        geneDocumentCache.setMolecularFunctionWithParents(getGOTermMap("molecular_function", false, species));

        checkMemory();
        log.info("Building gene -> whereExpressed map");
        geneDocumentCache.setWhereExpressed(getWhereExpressedMap(species));

        checkMemory();
        log.info("Building gene -> Subcellular Expression Ribbon map");
        geneDocumentCache.setSubcellularExpressionAgrSlim(getSubcellularExpressionAgrSlimMap(species));
        
        checkMemory();
        log.info("Building gene -> Subcellular Expression w/parents map");
        geneDocumentCache.setSubcellularExpressionWithParents(getSubcellularExpressionWithParentsMap(species));

        checkMemory();
        log.info("Building gene -> Expression Anatomy Ribbon map");
        geneDocumentCache.setAnatomicalExpression(getAnatomicalExpressionMap(species));
        
        checkMemory();
        log.info("Building gene -> Expression Anatomy w/parents map");
        geneDocumentCache.setAnatomicalExpressionWithParents(getAnatomicalExpressionWithParentsMap(species));

        checkMemory();
        return geneDocumentCache;
    }
    
    private void checkMemory() {
        log.info("Memory Warning: " + df.format(memoryPercent() * 100) + "%");
        log.info("Used Mem: " + (runtime.totalMemory() - runtime.freeMemory()));
        log.info("Free Mem: " + runtime.freeMemory());
        log.info("Total Mem: " + runtime.totalMemory());
        log.info("Max Memory: " + runtime.maxMemory());
    }

    private double memoryPercent() {
        return ((double) runtime.totalMemory() - (double) runtime.freeMemory()) / (double) runtime.maxMemory();
    }

    private Map<String, Set<String>> getSynonyms(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[:ALSO_KNOWN_AS]-(s:Synonym) ";
        query += getSpeciesWhere(species);
        query += " RETURN gene.primaryKey as id, s.name as value ";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

    private Map<String, Set<String>> getCrossReferences(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
        query += getSpeciesWhere(species);
        query += " RETURN gene.primaryKey as id, cr.name as value";

        Map<String, Set<String>> names = getMapSetForQuery(query, getSpeciesParams(species));

        query = "MATCH (species:Species)--(gene:Gene)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
        query += getSpeciesWhere(species);
        query += " RETURN gene.primaryKey as id, cr.localId as value";

        Map<String, Set<String>> localIds = getMapSetForQuery(query, getSpeciesParams(species));

        Map<String, Set<String>> map = new HashMap<>();
        Set<String> keys = new HashSet<>();

        keys.addAll(names.keySet());
        keys.addAll(localIds.keySet());

        for (String key: keys) {
            Set<String> values = new HashSet<>();
            values.addAll(names.get(key));
            values.addAll(localIds.get(key));
            map.put(key, values);
        }

        return map;
    }

    private Map<String, Set<String>> getChromosomes(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[:LOCATED_ON]-(c:Chromosome) ";
        query += getSpeciesWhere(species);
        query += " RETURN gene.primaryKey as id, c.primaryKey as value ";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

    private Map<String, Set<String>> getSecondaryIds(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[:ALSO_KNOWN_AS]-(s:SecondaryId) ";
        query += getSpeciesWhere(species);
        query += " RETURN gene.primaryKey as id, s.name as value ";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

    private Map<String, Set<String>> getAllelesMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[:IS_ALLELE_OF]-(allele:Allele) ";
        query += getSpeciesWhere(species);
        query += " RETURN gene.primaryKey as id,allele.symbolTextWithSpecies as value ";

        return getMapSetForQuery(query, "id", "value", getSpeciesParams(species));
    }

    private Map<String, Set<String>> getSoTermNameWithParentsMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[:ANNOTATED_TO]-(:SOTerm)-[:IS_A_PART_OF_CLOSURE]->(term:SOTerm) ";
        query += getSpeciesWhere(species);
        query += " RETURN gene.primaryKey as id, term.name as value";

        return getMapSetForQuery(query,"id","value", getSpeciesParams(species));
    }

    private Map<String, Set<String>> getSoTermNameAgrSlimMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[:ANNOTATED_TO]-(:SOTerm)-[:IS_A_PART_OF_CLOSURE]->(term:SOTerm) ";
        query += " WHERE not term.name in ['region'," +
                "'biological_region'," +
                "'sequence_feature']";
        if (StringUtils.isNotEmpty(species)) {
            query += " AND species.name = {species} ";
        }
        query += " RETURN gene.primaryKey as id, term.name as value";

        return getMapSetForQuery(query, "id", "value", getSpeciesParams(species));
    }

    private Map<String, Set<String>> getStrictOrthologySymbolsMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[o:ORTHOLOGOUS]-(orthoGene:Gene) WHERE o.strictFilter = true  ";
        query += getSpeciesWhere(species).replace("WHERE"," AND ");
        query += "RETURN distinct gene.primaryKey,orthoGene.symbol ";

        return getMapSetForQuery(query, "gene.primaryKey", "orthoGene.symbol", getSpeciesParams(species));
    }

    private Map<String, Set<String>> getDiseasesMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)--(:DiseaseEntityJoin)--(disease:DOTerm) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct gene.primaryKey, disease.nameKey ";

        return getMapSetForQuery(query, "gene.primaryKey", "disease.nameKey", getSpeciesParams(species));
    }

    private Map<String, Set<String>> getDiseasesAgrSlimMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)--(:DiseaseEntityJoin)--(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm) ";
        query += " WHERE disease.subset =~ '.*DO_AGR_slim.*' ";
        
        Map<String,String> params = new HashMap<String,String>();
        
        if (StringUtils.isNotEmpty(species)) {
            query += " AND species.name = {species} ";
            params.put("species",species);
        }
        query += " RETURN distinct gene.primaryKey, disease.nameKey ";

        return getMapSetForQuery(query, "gene.primaryKey", "disease.nameKey", params);
    }

    private Map<String, Set<String>> getDiseasesWithParents(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)--(:DiseaseEntityJoin)--(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct gene.primaryKey, disease.nameKey ";

        return getMapSetForQuery(query, "gene.primaryKey", "disease.nameKey", getSpeciesParams(species));
    }

    private Map<String,Set<String>> getModelMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)-[:MODEL_COMPONENT|:SEQUENCE_TARGETING_REAGENT]-(feature)--(gene:Gene)";
        query += getSpeciesWhere(species);
        query += " RETURN gene.primaryKey as id, model.nameTextWithSpecies as value";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

    private Map<String,Set<String>> getPhenotypeStatementMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)--(phenotype:Phenotype) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct gene.primaryKey, phenotype.phenotypeStatement ";
        return getMapSetForQuery(query, "gene.primaryKey", "phenotype.phenotypeStatement", getSpeciesParams(species));
    }

    private Map<String,Set<String>> getGOTermMap(String type, Boolean slim, String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(:GOTerm)-[:IS_A_PART_OF_CLOSURE|IS_A_PART_OF_SELF_CLOSURE]->(term:GOTerm) ";
        query += "WHERE term.type = {type}";
        
        Map<String,String> params = new HashMap<String,String>();
        
        if (StringUtils.isNotEmpty(species)) {
            query += " AND species.name = {species} ";
            params.put("species",species);
        }
        if (slim) {
            query += " AND term.subset =~ '.*goslim_agr.*' ";
        }
        query += " RETURN distinct gene.primaryKey, term.name";
        
        params.put("type", type);

        return getMapSetForQuery(query, "gene.primaryKey", "term.name", params);
    }

    public Map<String, Set<String>> getWhereExpressedMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct gene.primaryKey as id, ebe.whereExpressedStatement as value";

        return getMapSetForQuery(query, "id", "value", getSpeciesParams(species));
    }

    public Map<String,Set<String>> getSubcellularExpressionAgrSlimMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity)-[:CELLULAR_COMPONENT_RIBBON_TERM]->(term:GOTerm) ";
        query += getSpeciesWhere(species);
        query +=  " RETURN distinct gene.primaryKey, term.name ";

        return getMapSetForQuery(query, "gene.primaryKey", "term.name", getSpeciesParams(species));
    }

    public Map<String,Set<String>> getSubcellularExpressionWithParentsMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity)-[:CELLULAR_COMPONENT]-(:GOTerm)-[:IS_A_PART_OF_CLOSURE|IS_A_PART_OF_SELF_CLOSURE]->(term:GOTerm) ";
        query += getSpeciesWhere(species);
        query +=  " RETURN distinct gene.primaryKey, term.name ";

        return getMapSetForQuery(query, "gene.primaryKey", "term.name", getSpeciesParams(species));
    }

    public Map<String,Set<String>> getAnatomicalExpressionMap(String species) {
        String query = " MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity)-[:ANATOMICAL_RIBBON_TERM]-(term:Ontology) ";
        query += getSpeciesWhere(species);
        query +=  " RETURN distinct gene.primaryKey, term.name ";

        return getMapSetForQuery(query, "gene.primaryKey", "term.name", getSpeciesParams(species));
    }

    public Map<String,Set<String>> getAnatomicalExpressionWithParentsMap(String species) {
        String query = " MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity)-[:ANATOMICAL_STRUCTURE]-(:Ontology)-[:IS_A_PART_OF_CLOSURE|IS_A_PART_OF_SELF_CLOSURE]->(term:Ontology) ";
        query += getSpeciesWhere(species);
        query +=  " RETURN distinct gene.primaryKey, term.name ";

        return getMapSetForQuery(query, "gene.primaryKey", "term.name", getSpeciesParams(species));
    }

}
