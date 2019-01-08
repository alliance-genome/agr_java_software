package org.alliancegenome.neo4j.repository;

import org.alliancegenome.es.index.site.cache.GeneDocumentCache;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.model.Result;

import java.util.*;

public class GeneIndexerRepository extends Neo4jRepository<Gene>  {

    private final Logger log = LogManager.getLogger(getClass());

    public GeneIndexerRepository() {
        super(Gene.class);
    }

    public Map<String,Gene> getGeneMap(String species) {

        String query = " MATCH p1=(species:Species)-[:FROM_SPECIES]-(g:Gene) ";
        query += getSpeciesWhere(species);
        query += " OPTIONAL MATCH pSyn=(g:Gene)-[:ALSO_KNOWN_AS]-(:Synonym) ";
        query += " OPTIONAL MATCH pCR=(g:Gene)-[:CROSS_REFERENCE]-(:CrossReference)";
        query += " OPTIONAL MATCH pChr=(g:Gene)-[:LOCATED_ON]-(:Chromosome)";
        query += " RETURN p1, pSyn, pCR, pChr";

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

    public Gene getIndexableGene(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";

        query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene) WHERE g.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH pSyn=(g:Gene)-[:ALSO_KNOWN_AS]-(:Synonym) ";
        query += " OPTIONAL MATCH pCR=(g:Gene)-[:CROSS_REFERENCE]-(:CrossReference)";
        query += " OPTIONAL MATCH pChr=(g:Gene)-[:LOCATED_ON]-(:Chromosome)";
        query += " RETURN p1, pSyn, pCR, pChr";

        Iterable<Gene> genes = query(query, map);
        for (Gene g : genes) {
            if (g.getPrimaryKey().equals(primaryKey)) {
                return g;
            }
        }

        return null;
    }

    public GeneDocumentCache getGeneDocumentCache() {
        return getGeneDocumentCache(null);
    }

    public GeneDocumentCache getGeneDocumentCache(String species) {
        GeneDocumentCache geneDocumentCache = new GeneDocumentCache();

        log.info("Fetching genes");
        geneDocumentCache.setGeneMap(getGeneMap(species));

        log.info("Building gene -> alleles map");
        geneDocumentCache.setAlleles(getAllelesMap(species));

        log.info("Building gene -> strictOrthologySymbols map");
        geneDocumentCache.setStrictOrthologySymbols(getStrictOrthologySymbolsMap(species));

        log.info("Building gene -> diseases map");
        geneDocumentCache.setDiseases(getDiseasesMap(species));

        log.info("Building gene -> phenotypeStatement map");
        geneDocumentCache.setPhenotypeStatements(getPhenotypeStatementMap(species));

        log.info("Building gene -> GO BP Slim map");
        geneDocumentCache.setBiologicalProcessAgrSlim(getGOTermMap("biological_process", true, species));
        log.info("Building gene -> GO CC Slim map");
        geneDocumentCache.setBiologicalProcessWithParents(getGOTermMap("cellular_component", true, species));
        log.info("Building gene -> GO MF Slim map");
        geneDocumentCache.setMolecularFunctionAgrSlim(getGOTermMap("molecular_function", true, species));

        log.info("Building gene -> GO BP w/parents map");
        geneDocumentCache.setBiologicalProcessWithParents(getGOTermMap("biological_process", false, species));
        log.info("Building gene -> GO CC w/parents map");
        geneDocumentCache.setCellularComponentWithParents(getGOTermMap("cellular_component", false, species));
        log.info("Building gene -> GO MF w/parents map");
        geneDocumentCache.setMolecularFunctionWithParents(getGOTermMap("molecular_function", false, species));

        log.info("Building gene -> whereExpressed map");
        geneDocumentCache.setWhereExpressed(getWhereExpressedMap(species));

        log.info("Building gene -> Expression GO CC Ribbon map");
        geneDocumentCache.setCellularComponentAgrSlim(getCellularComponentExpressionAgrSlimMap(species));
        log.info("Building gene -> Expression GO CC w/parents map");
        geneDocumentCache.setCellularComponentExpressionWithParents(getCellularComponentExpressionWithParentsMap(species));

        log.info("Building gene -> Expression Anatomy Ribbon map");
        geneDocumentCache.setAnatomicalExpression(getAnatomicalExpressionMap(species));
        log.info("Building gene -> Expression Anatomy w/parents map");
        geneDocumentCache.setAnatomicalExpressionWithParents(getAnatomicalExpressionWithParentsMap(species));


        return geneDocumentCache;
    }


    private Map<String, Set<String>> getAllelesMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[:IS_ALLELE_OF]-(feature:Feature) ";
        query += getSpeciesWhere(species);
        query += " RETURN gene.primaryKey,feature.symbolText ";

        return getMapSetForQuery(query, "gene.primaryKey", "feature.symbolText", getSpeciesParams(species));
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

        return getMapSetForQuery(query, "gene.primaryKey", "", getSpeciesParams(species));
    }


    private Map<String,Set<String>> getPhenotypeStatementMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)--(phenotype:Phenotype) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct gene.primaryKey, phenotype.phenotypeStatement ";
        return getMapSetForQuery(query, "gene.primaryKey", "phenotype.phenotypeStatement", getSpeciesParams(species));
    }

    private Map<String,Set<String>> getGOTermMap(String type, Boolean slim, String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(:GOTerm)-[:COMPUTED_CLOSURE]->(term:GOTerm) ";
        query += "WHERE term.type = {type}";
        if (StringUtils.isNotEmpty(species)) {
            query += " AND species.name = {species} ";
        }
        if (slim) {
            query += " AND 'goslim_agr' IN term.subset ";
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
        query += " RETURN distinct gene.primaryKey, ebe.whereExpressed";

        return getMapSetForQuery(query, "gene.primaryKey", "ebe.whereExpressed", getSpeciesParams(species));
    }

    public Map<String,Set<String>> getCellularComponentExpressionAgrSlimMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity)-[:CELLULAR_COMPONENT_RIBBON_TERM]-(:GOTerm)-[:COMPUTED_CLOSURE]->(term:GOTerm) ";
        query += getSpeciesWhere(species);
        query +=  " RETURN distinct gene.primaryKey, term.name ";

        return getMapSetForQuery(query, "gene.primaryKey", "term.name", getSpeciesParams(species));
    }

    public Map<String,Set<String>> getCellularComponentExpressionWithParentsMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity)-[:CELLULAR_COMPONENT]-(:GOTerm)-[:COMPUTED_CLOSURE]->(term:GOTerm) ";
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
        String query = " MATCH (species:Species)-[:FROM_SPECIES]-(gene:Gene)--(ebe:ExpressionBioEntity)-[:ANATOMICAL_STRUCTURE]-(:Ontology)-[:COMPUTED_CLOSURE]->(term:Ontology) ";
        query += getSpeciesWhere(species);
        query +=  " RETURN distinct gene.primaryKey, term.name ";

        return getMapSetForQuery(query, "gene.primaryKey", "term.name", getSpeciesParams(species));
    }

}
