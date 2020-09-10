package org.alliancegenome.neo4j.repository;

import org.alliancegenome.neo4j.entity.node.Allele;
import org.neo4j.ogm.model.Result;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AlleleRepository extends Neo4jRepository<Allele> {

    public AlleleRepository() {
        super(Allele.class);
    }

    public Allele getAllele(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";
        query += " MATCH p1=(aSpecies:Species)-[:FROM_SPECIES]-(a:Allele) WHERE a.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH p3=(a:Allele)-[:IS_ALLELE_OF]-(gene:Gene)-[:FROM_SPECIES]-(gSpecies:Species)";
        query += " OPTIONAL MATCH p4=(a:Allele)-[:ALSO_KNOWN_AS]-(:Synonym)";
        query += " OPTIONAL MATCH vari=(a:Allele)<-[:VARIATION]-(variant:Variant)-[:VARIATION_TYPE]-(soTerm:SOTerm)";
        query += " OPTIONAL MATCH crossRef=(a:Allele)-[:CROSS_REFERENCE]-(:CrossReference)";
        query += " OPTIONAL MATCH construct=(a:Allele)-[:CONTAINS]-(con:Construct)";
        query += " OPTIONAL MATCH crossRefCon=(con:Construct)-[:CROSS_REFERENCE]-(:CrossReference)";
        query += " OPTIONAL MATCH regGene=(con:Construct)<-[:IS_REGULATED_BY]-(:Gene)-[:FROM_SPECIES]->(:Species)";
        query += " OPTIONAL MATCH expGene=(con:Construct)-[:EXPRESSES]-(:Gene)-[:FROM_SPECIES]->(:Species)";
        query += " OPTIONAL MATCH expNonBgiCC=(con:Construct)-[:EXPRESSES]-(:NonBGIConstructComponent)";
        query += " OPTIONAL MATCH expNonBgiCCRegulation=(con:Construct)-[:IS_REGULATED_BY]-(:NonBGIConstructComponent)";
        query += " OPTIONAL MATCH expNonBgiCCTarget=(con:Construct)-[:TARGETS]-(:NonBGIConstructComponent)";
        query += " OPTIONAL MATCH targetGene=(con:Construct)-[:TARGETS]-(:Gene)-[:FROM_SPECIES]->(:Species)";
        query += " OPTIONAL MATCH p2=(gene:Gene)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
        query += " RETURN p1, p2, p3, p4, crossRef, construct, regGene, vari, expGene, targetGene, crossRefCon, expNonBgiCC, expNonBgiCCRegulation, expNonBgiCCTarget";

        Iterable<Allele> alleles = query(query, map);
        for (Allele a : alleles) {
            if (a.getPrimaryKey().equals(primaryKey)) {
                return a;
            }
        }

        return null;
    }

    public List<String> getAllAlleleKeys() {
        String query = "MATCH (a:Allele)--(g:Gene)-[:FROM_SPECIES]-(q:Species) RETURN a.primaryKey";

        Result r = queryForResult(query);
        Iterator<Map<String, Object>> i = r.iterator();

        ArrayList<String> list = new ArrayList<>();

        while (i.hasNext()) {
            Map<String, Object> map2 = i.next();
            list.add((String) map2.get("a.primaryKey"));
        }
        return list;
    }

    public Set<Allele> getAllAlleles() {
        HashMap<String, String> map = new HashMap<>();

        String query = "";
        query += " MATCH p1=(:Species)<-[:FROM_SPECIES]-(a:Allele) ";
        //query += " where g.primaryKey = 'FB:FBgn0002121' AND a.primaryKey = 'FB:FBal0051412' ";
        //query += " where a.primaryKey in ['MGI:3795217','MGI:3712283','MGI:3843784','MGI:2158359'] ";
        query += " OPTIONAL MATCH gene=(a:Allele)-[:IS_ALLELE_OF]->(g:Gene)-[:FROM_SPECIES]-(q:Species)";
        query += " OPTIONAL MATCH vari=(a:Allele)<-[:VARIATION]-(variant:Variant)--(soTerm:SOTerm)";
        query += " OPTIONAL MATCH consequence=(:GeneLevelConsequence)<-[:ASSOCIATION]-(variant:Variant)";
        query += " OPTIONAL MATCH loc=(variant:Variant)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
        query += " OPTIONAL MATCH p2=(a:Allele)-[:ALSO_KNOWN_AS]->(synonym:Synonym)";
        query += " OPTIONAL MATCH crossRef=(a:Allele)-[:CROSS_REFERENCE]->(c:CrossReference)";
        query += " OPTIONAL MATCH disease=(a:Allele)<-[:IS_IMPLICATED_IN]-(doTerm:DOTerm)";
        query += " OPTIONAL MATCH pheno=(a:Allele)-[:HAS_PHENOTYPE]->(ph:Phenotype)";
        query += " OPTIONAL MATCH construct=(a:Allele)-[:CONTAINS]->(:Construct)";
        query += " RETURN p1, p2, vari, crossRef, disease, pheno, loc, consequence, gene, construct ";

        Iterable<Allele> alleles = query(query, map);
        return StreamSupport.stream(alleles.spliterator(), false)
                .collect(Collectors.toSet());
    }

    /*
     * Need to run 3 queries as a union as the where clause is against the gene of
     * three typed associations (EXPRESSES, TARGET, IS_REGULATED_BY) against the construct node
     * but there might be more genes with the same association
     */
    public List<Allele> getTransgenicAlleles(String geneID) {
        HashMap<String, String> map = new HashMap<>();
        map.put("geneID", geneID);
        String query = getCypherQuery("EXPRESSES");

        Iterable<Allele> alleles = query(query, map);
        List<Allele> alleleList = StreamSupport.stream(alleles.spliterator(), false)
                .collect(Collectors.toList());

        query = getCypherQuery("TARGETS");

        alleles = query(query, map);
        alleleList.addAll(StreamSupport.stream(alleles.spliterator(), false)
                .collect(Collectors.toList()));

        alleleList.sort(Comparator.comparing(Allele::getSymbolText));
        return alleleList;
    }

    private String getCypherQuery(String relationship) {
        String query = "";
        query += " MATCH p1=(:Species)<-[:FROM_SPECIES]-(allele:Allele)--(construct:Construct)-[:" + relationship + "]-(gene:Gene)--(:Species) " +
                "  where gene.primaryKey = {geneID}";
        // need this optional match to retrieve all expresses genes besides the given geneID
        query += " OPTIONAL MATCH express=(:CrossReference)--(construct:Construct)-[:EXPRESSES]-(:Gene)--(:Species)";
        query += " OPTIONAL MATCH expressNonBGI=(:CrossReference)--(construct:Construct)-[:EXPRESSES]-(:NonBGIConstructComponent)";
        query += " OPTIONAL MATCH target=(:CrossReference)--(construct:Construct)-[:TARGET]-(:Gene)--(:Species)";
        query += " OPTIONAL MATCH targetNon=(:CrossReference)--(construct:Construct)-[:TARGET]-(:NonBGIConstructComponent)";
        query += " OPTIONAL MATCH regulated=(:CrossReference)--(construct:Construct)-[:IS_REGULATED_BY]-(:Gene)--(:Species)";
        query += " OPTIONAL MATCH regulatedNon=(:CrossReference)--(construct:Construct)-[:IS_REGULATED_BY]-(:NonBGIConstructComponent)";
        query += " OPTIONAL MATCH disease=(allele:Allele)--(:DiseaseEntityJoin)";
        query += " OPTIONAL MATCH pheno=(allele:Allele)-[:HAS_PHENOTYPE]-(:Phenotype)";
        query += " RETURN p1, express, target, regulated, expressNonBGI, regulatedNon, targetNon, disease, pheno ";
        return query;
    }
}
