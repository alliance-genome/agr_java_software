package org.alliancegenome.neo4j.repository;

import java.text.DecimalFormat;
import java.util.HashMap;
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
        query += " OPTIONAL MATCH pSyn=(g:Gene)-[:ALSO_KNOWN_AS]-(:Synonym) ";
        query += " OPTIONAL MATCH pCR=(g:Gene)-[:CROSS_REFERENCE]-(:CrossReference)";
        query += " OPTIONAL MATCH pChr=(g:Gene)-[:LOCATED_ON]-(:Chromosome)";
        query += " OPTIONAL MATCH pSecondaryId=(g:Gene)-[:ALSO_KNOWN_AS]-(s:SecondaryId)";
        query += " OPTIONAL MATCH pSoTerm=(g:Gene)-[:ANNOTATED_TO]-(soTerm:SOTerm)";
        query += " RETURN p1, pSyn, pCR, pChr, pSecondaryId, pSoTerm";

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
        log.info("Building gene -> Expression GO CC Ribbon map");
        geneDocumentCache.setCellularComponentExpressionAgrSlim(getCellularComponentExpressionAgrSlimMap(species));
        
        checkMemory();
        log.info("Building gene -> Expression GO CC w/parents map");
        geneDocumentCache.setCellularComponentExpressionWithParents(getCellularComponentExpressionWithParentsMap(species));

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

    private Map<String, Set<String>> getAllelesMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[:IS_ALLELE_OF]-(allele:Allele) ";
        query += getSpeciesWhere(species);
        query += " RETURN gene.primaryKey as id,allele.symbolText as value ";

        return getMapSetForQuery(query, "id", "value", getSpeciesParams(species));
    }

    private Map<String, Set<String>> getSoTermNameWithParentsMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[:ANNOTATED_TO]-(:SOTerm)-[:IS_A_PART_OF_CLOSURE]->(term:SOTerm) ";
        query += getSpeciesWhere(species);
        query += " RETURN gene.primaryKey as id, term.name as value";

        return getMapSetForQuery(query,"id","value", getSpeciesParams(species));
    }

    //todo: some kind of slimming, possibly with manual filtering
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
        String query = "MATCH (species:Species)--(gene:Gene)-[:IS_IMPLICATED_IN]-(disease:DOTerm) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct gene.primaryKey, disease.nameKey ";

        return getMapSetForQuery(query, "gene.primaryKey", "disease.nameKey", getSpeciesParams(species));
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
        if (StringUtils.isNotEmpty(species)) {
            query += " AND species.name = {species} ";
        }
        if (slim) {
            query += " AND term.subset =~ '.*goslim_agr.*' ";
        }
        query += " RETURN distinct gene.primaryKey, term.name";
        
        Map<String,String> params = new HashMap<String,String>();
        params.put("type",type);
        if (StringUtils.isNotEmpty(species)) {
            params.put("species",species);
        }

        return getMapSetForQuery(query, "gene.primaryKey", "term.name", params);
    }

    public Map<String, Set<String>> getWhereExpressedMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct gene.primaryKey as id, ebe.whereExpressedStatement as value";

        return getMapSetForQuery(query, "id", "value", getSpeciesParams(species));
    }

    public Map<String,Set<String>> getCellularComponentExpressionAgrSlimMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity)-[:CELLULAR_COMPONENT_RIBBON_TERM]->(term:GOTerm) ";
        query += getSpeciesWhere(species);
        query +=  " RETURN distinct gene.primaryKey, term.name ";

        return getMapSetForQuery(query, "gene.primaryKey", "term.name", getSpeciesParams(species));
    }

    public Map<String,Set<String>> getCellularComponentExpressionWithParentsMap(String species) {
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
