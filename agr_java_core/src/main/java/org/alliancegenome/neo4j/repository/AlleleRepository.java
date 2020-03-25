package org.alliancegenome.neo4j.repository;

import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Variant;
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
        query += " MATCH p1=(:Species)-[:FROM_SPECIES]-(a:Allele) WHERE a.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH p3=(a:Allele)-[:IS_ALLELE_OF]-(gene:Gene)-[:FROM_SPECIES]-(:Species)";
        query += " OPTIONAL MATCH p4=(a:Allele)-[:ALSO_KNOWN_AS]-(:Synonym)";
        query += " OPTIONAL MATCH crossRef=(a:Allele)-[:CROSS_REFERENCE]-(:CrossReference)";
        query += " OPTIONAL MATCH p2=(gene:Gene)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
        query += " RETURN p1, p2, p3, p4, crossRef";

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
        //query += " where g.primaryKey = 'FB:FBgn0025832' ";
        query += " OPTIONAL MATCH gene=(a:Allele)-[:IS_ALLELE_OF]->(g:Gene)-[:FROM_SPECIES]-(q:Species)";
        query += " OPTIONAL MATCH vari=(a:Allele)<-[:VARIATION]-(variant:Variant)--(soTerm:SOTerm)";
        query += " OPTIONAL MATCH consequence=(:GeneLevelConsequence)<-[:ASSOCIATION]-(variant:Variant)";
        query += " OPTIONAL MATCH loc=(variant:Variant)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
        query += " OPTIONAL MATCH p2=(a:Allele)-[:ALSO_KNOWN_AS]->(synonym:Synonym)";
        query += " OPTIONAL MATCH crossRef=(a:Allele)-[:CROSS_REFERENCE]->(c:CrossReference)";
        query += " OPTIONAL MATCH disease=(a:Allele)<-[:IS_IMPLICATED_IN]-(doTerm:DOTerm)";
        query += " OPTIONAL MATCH pheno=(a:Allele)-[:HAS_PHENOTYPE]->(ph:Phenotype)";
        query += " RETURN p1, p2, vari, crossRef, disease, pheno, loc, consequence, gene ";

        Iterable<Allele> alleles = query(query, map);
        return StreamSupport.stream(alleles.spliterator(), false)
                .collect(Collectors.toSet());
    }

    public List<Variant> getVariants(String id) {
        HashMap<String, String> map = new HashMap<>();
        String paramName = "alleleID";
        map.put(paramName, id);
        String query = "";
        query += " MATCH p1=(a:Allele)<-[:VARIATION]-(variant:Variant)--(soTerm:SOTerm) ";
        query += " WHERE a.primaryKey = {" + paramName + "}";
        query += " OPTIONAL MATCH consequence=(:GeneLevelConsequence)<-[:ASSOCIATION]-(variant:Variant)";
        query += " OPTIONAL MATCH loc=(variant:Variant)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
        query += " OPTIONAL MATCH p2=(variant:Variant)<-[:COMPUTED_GENE]-(:Gene)-[:ASSOCIATION]->(:GenomicLocation)-[:ASSOCIATION]->(:Chromosome)";
        query += " RETURN p1, p2, loc, consequence ";

        Iterable<Variant> alleles = query(Variant.class, query, map);
        return StreamSupport.stream(alleles.spliterator(), false)
                .collect(Collectors.toList());


    }
}
