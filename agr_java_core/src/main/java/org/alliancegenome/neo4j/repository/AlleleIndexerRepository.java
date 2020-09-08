package org.alliancegenome.neo4j.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.es.index.site.cache.AlleleDocumentCache;
import org.alliancegenome.es.util.CollectionHelper;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AlleleIndexerRepository extends Neo4jRepository {

    private final Logger log = LogManager.getLogger(getClass());

    public AlleleIndexerRepository() { super(Allele.class); }

    public Map<String, Allele> getAlleleMap(String species) {

        String query = "MATCH p1=(species:Species)-[:FROM_SPECIES]-(feature:Allele) ";
        query += getSpeciesWhere(species);
        query += " OPTIONAL MATCH pSyn=(feature:Feature)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";
        query += " RETURN p1, pSyn ";

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

        log.info("Fetching allele -> constructs map");
        alleleDocumentCache.setConstructs(getConstructs(species));

        log.info("Fetching allele -> constructExpressedComponent map");
        alleleDocumentCache.setConstructExpressedComponents(getConstructExpressedComponents(species));

        log.info("Fetching allele -> constructKnockdownComponent map");
        alleleDocumentCache.setConstructKnockdownComponents(getConstructKnockdownComponent(species));

        log.info("Fetching allele -> constructRegulatoryRegion map");
        alleleDocumentCache.setConstructRegulatoryRegions(getConstructRegulatoryRegions(species));

        log.info("Building allele -> crossReference map");
        alleleDocumentCache.setCrossReferences(getCrossReferences(species));

        log.info("Building allele -> diseases map");
        alleleDocumentCache.setDiseases(getDiseaseMap(species));

        log.info("Building allele -> diseasesAgrSlim map");
        alleleDocumentCache.setDiseasesAgrSlim(getDiseasesAgrSlimMap(species));

        log.info("Building allele -> diseasesWithParents map");
        alleleDocumentCache.setDiseasesWithParents(getDiseasesWithParents(species));

        log.info("Building allele -> genes map");
        alleleDocumentCache.setGenes(getGenesMap(species));

        log.info("Building allele -> model map");
        alleleDocumentCache.setModels(getModels(species));

        log.info("Building allele -> phenotype statements map");
        alleleDocumentCache.setPhenotypeStatements(getPhenotypeStatementsMap(species));

        log.info("Building allele -> variant name map");
        alleleDocumentCache.setVariants(getVariants(species));

        log.info("Building allele -> variant synonyms map");
        alleleDocumentCache.setVariantSynonyms(getVariantSynonyms(species));

        log.info("Building allele -> variant types map");
        alleleDocumentCache.setVariantType(getVariantTypeMap(species));

        log.info("Building allele -> molecular consequence map");
        alleleDocumentCache.setMolecularConsequenceMap(getMolecularConsequence(species));

        return alleleDocumentCache;

    }

    private Map<String, Set<String>> getCrossReferences(String species) {
        String query = "MATCH (species:Species)--(allele:Allele)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
        query += getSpeciesWhere(species);
        query += " RETURN allele.primaryKey as id, cr.name as value";

        Map<String, Set<String>> names = getMapSetForQuery(query, getSpeciesParams(species));

        query = "MATCH (species:Species)--(allele:Allele)-[:CROSS_REFERENCE]-(cr:CrossReference) ";
        query += getSpeciesWhere(species);
        query += " RETURN allele.primaryKey as id, cr.localId as value";

        Map<String, Set<String>> localIds = getMapSetForQuery(query, getSpeciesParams(species));

        return CollectionHelper.merge(names, localIds);
    }

    public Map<String, Set<String>> getConstructs(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct) ";
        query += getSpeciesWhere(species);
        query += " RETURN allele.primaryKey as id, [construct.nameText, construct.primaryKey] as value";

        Map<String, Set<String>> constructs = getMapSetForQuery(query, getSpeciesParams(species));

        query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";
        query += getSpeciesWhere(species);
        query += "RETURN allele.primaryKey as id, synonym.name as value ";

        constructs = CollectionHelper.merge(constructs, getMapSetForQuery(query, getSpeciesParams(species)));

        query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:CROSS_REFERENCE]-(crossReference:CrossReference) ";
        query += getSpeciesWhere(species);
        query += "RETURN allele.primaryKey as id, crossReference.name as value ";

        constructs = CollectionHelper.merge(constructs, getMapSetForQuery(query, getSpeciesParams(species)));

        query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:ALSO_KNOWN_AS]-(secondaryId:SecondaryId) ";
        query += getSpeciesWhere(species);
        query += " RETURN allele.primaryKey as id, secondaryId.name as value";

        constructs = CollectionHelper.merge(constructs, getMapSetForQuery(query, getSpeciesParams(species)));

        query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:EXPRESSES|TARGETS|IS_REGULATED_BY]-(gene:Gene) ";
        query += getSpeciesWhere(species);
        query += " RETURN allele.primaryKey as id, gene.primaryKey as value; ";

        constructs = CollectionHelper.merge(constructs, getMapSetForQuery(query, getSpeciesParams(species)));

        return constructs;
    }

    public Map<String, Set<String>> getConstructExpressedComponents(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:EXPRESSES]-(constructComponent:NonBGIConstructComponent) ";
        query += getSpeciesWhere(species);
        query += " RETURN allele.primaryKey as id, constructComponent.primaryKey as value";

        Map<String, Set<String>> result = getMapSetForQuery(query, getSpeciesParams(species));

        query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:EXPRESSES]-(gene:Gene) ";
        query += getSpeciesWhere(species);
        query += " RETURN allele.primaryKey as id, gene.symbolWithSpecies as value; ";

        return CollectionHelper.merge(result, getMapSetForQuery(query, getSpeciesParams(species)));
    }

    public Map<String, Set<String>> getConstructKnockdownComponent(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:TARGETS]-(constructComponent:NonBGIConstructComponent) ";
        query += getSpeciesWhere(species);
        query += " RETURN allele.primaryKey as id, constructComponent.primaryKey as value";

        Map<String, Set<String>> result = getMapSetForQuery(query, getSpeciesParams(species));

        query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:TARGETS]-(gene:Gene) ";
        query += getSpeciesWhere(species);
        query += " RETURN allele.primaryKey as id, gene.symbolWithSpecies as value; ";

        return CollectionHelper.merge(result, getMapSetForQuery(query, getSpeciesParams(species)));
    }

    public Map<String, Set<String>> getConstructRegulatoryRegions(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:IS_REGULATED_BY]-(constructComponent:NonBGIConstructComponent) ";
        query += getSpeciesWhere(species);
        query += " RETURN allele.primaryKey as id, constructComponent.primaryKey as value";

        Map<String, Set<String>> result = getMapSetForQuery(query, getSpeciesParams(species));

        query = "MATCH (species:Species)-[:FROM_SPECIES]-(allele:Allele)-[:CONTAINS]-(construct:Construct)-[:IS_REGULATED_BY]-(gene:Gene) ";
        query += getSpeciesWhere(species);
        query += " RETURN allele.primaryKey as id, gene.symbolWithSpecies as value; ";

        return CollectionHelper.merge(result, getMapSetForQuery(query, getSpeciesParams(species)));
    }

    public Map<String, Set<String>> getDiseaseMap(String species) {
        String query = "MATCH (species:Species)--(a:Allele)--(disease:DOTerm) ";
        query += getSpeciesWhere(species);
        query += " RETURN a.primaryKey, disease.nameKey ";

        return getMapSetForQuery(query, "a.primaryKey", "disease.nameKey", getSpeciesParams(species));
    }


    public Map<String, Set<String>> getDiseasesAgrSlimMap(String species) {
        String query = "MATCH (species:Species)--(a:Allele)--(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm)";
        query += " WHERE disease.subset =~ '.*DO_AGR_slim.*' ";
        if (StringUtils.isNotEmpty(species)) {
            query += " AND species.name = {species} ";
        }
        query += " RETURN a.primaryKey, disease.nameKey ";

        Map<String,String> params = new HashMap<String,String>();
        if (StringUtils.isNotEmpty(species)) {
            params.put("species",species);
        }

        return getMapSetForQuery(query, "a.primaryKey", "disease.nameKey", params);
    }

    public Map<String, Set<String>> getDiseasesWithParents(String species) {
        String query = "MATCH (species:Species)--(a:Allele)--(:DOTerm)-[:IS_A_PART_OF_CLOSURE]->(disease:DOTerm)";
        query += getSpeciesWhere(species);
        query += " RETURN a.primaryKey, disease.nameKey ";

        return getMapSetForQuery(query, "a.primaryKey", "disease.nameKey", getSpeciesParams(species));
    }

    public Map<String, Set<String>> getGenesMap(String species) {
        String query = "MATCH (species:Species)--(gene:Gene)-[:IS_ALLELE_OF]-(a:Allele) ";
        query += getSpeciesWhere(species);
        query += "RETURN distinct a.primaryKey, gene.symbolWithSpecies";

        return getMapSetForQuery(query, "a.primaryKey", "gene.symbolWithSpecies", getSpeciesParams(species));
    }

    public Map<String, Set<String>> getModels(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(model:AffectedGenomicModel)--(allele:Allele)";
        query += getSpeciesWhere(species);
        query += " RETURN allele.primaryKey as id, model.nameTextWithSpecies as value";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

    public Map<String, Set<String>> getPhenotypeStatementsMap(String species) {
        String query = "MATCH (species:Species)--(a:Allele)--(phenotype:Phenotype) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct a.primaryKey, phenotype.phenotypeStatement ";

        return getMapSetForQuery(query, "a.primaryKey", "phenotype.phenotypeStatement", getSpeciesParams(species));
    }

    public Map<String, Set<String>> getVariants(String species) {
        String query = " MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct a.primaryKey as id, v.name as value";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

    public Map<String, Set<String>> getVariantSynonyms(String species) {

        String query = " MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:ASSOCIATION]-(tlc:TranscriptLevelConsequence)  ";
        query += getSpeciesWhere(species);
        query += "RETURN distinct a.primaryKey as id, [v.hgvsNomenclature, tlc.hgvsVEPGeneNomenclature, tlc.hgvsProteinNomenclature, tlc.hgvsCodingNomenclature] as value  ";
        Map<String,Set<String>> tlcNames = getMapSetForQuery(query, getSpeciesParams(species));

        query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:ALSO_KNOWN_AS]-(synonym:Synonym) ";
        query += getSpeciesWhere(species);
        query += " RETURN a.primaryKey as id, synonym.name as value ";
        Map<String,Set<String>> synonyms = getMapSetForQuery(query, getSpeciesParams(species));

        return CollectionHelper.merge(tlcNames, synonyms);
    }

    public Map<String, Set<String>> getVariantTypeMap(String species) {
        String query = "MATCH (species:Species)-[:FROM_SPECIES]-(a:Allele)-[:VARIATION]-(v:Variant)-[:VARIATION_TYPE]-(term:SOTerm) ";
        query += getSpeciesWhere(species);
        query += " RETURN distinct a.primaryKey,term.name ";

        return getMapSetForQuery(query, "a.primaryKey", "term.name", getSpeciesParams(species));
    }

    public Map<String, Set<String>> getMolecularConsequence(String species) {
        String query = "MATCH (species:Species)--(a:Allele)--(v:Variant)--(consequence:GeneLevelConsequence) ";
        query += getSpeciesWhere(species);
        query += " RETURN a.primaryKey as id, consequence.geneLevelConsequence as value ";

        return getMapSetForQuery(query, getSpeciesParams(species));
    }

}
