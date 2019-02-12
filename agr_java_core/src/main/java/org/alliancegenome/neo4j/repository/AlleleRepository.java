package org.alliancegenome.neo4j.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alliancegenome.neo4j.entity.node.Allele;
import org.neo4j.ogm.model.Result;

public class AlleleRepository extends Neo4jRepository<Allele> {

    public AlleleRepository() {
        super(Allele.class);
    }

    public Allele getAllele(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";
        query += " MATCH p1=(a:Allele)-[:IS_ALLELE_OF]-(g:Gene)-[:FROM_SPECIES]-(:Species) WHERE a.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH p2=(a:Allele)-[:ASSOCIATION]-(diseaseJoin:DiseaseEntityJoin)-[:ASSOCIATION]-(do:DOTerm)";
        query += " OPTIONAL MATCH p3=(do:DOTerm)-[:ASSOCIATION]-(diseaseJoin:DiseaseEntityJoin)-[:EVIDENCE]-(ea)";
        query += " OPTIONAL MATCH p4=(a:Allele)-[:ALSO_KNOWN_AS]-(synonym:Synonym)";
        query += " OPTIONAL MATCH p5=(a:Allele)-[:ASSOCIATION]-(diseaseJoin:DiseaseEntityJoin)-[:ASSOCIATION]-(g:Gene)";
        query += " OPTIONAL MATCH p6=(a:Allele)-[:HAS_PHENOTYPE]-(termName:PhenotypeAPI)";
        query += " OPTIONAL MATCH crossRef=(a:Allele)-[:CROSS_REFERENCE]-(c:CrossReference)";
        query += " RETURN p1, p2, p3, p4, p5, p6, crossRef";
        
        Iterable<Allele> alleles = query(query, map);
        for (Allele a: alleles) {
            if (a.getPrimaryKey().equals(primaryKey)) {
                return a;
            }
        }

        return null;
    }


    public List<String> getAllFeatureKeys() {
        String query = "MATCH (a:Allele)--(g:Gene)-[:FROM_SPECIES]-(q:Species) RETURN a.primaryKey";

        Result r = queryForResult(query);
        Iterator<Map<String, Object>> i = r.iterator();

        ArrayList<String> list = new ArrayList<>();

        while (i.hasNext()) {
            Map<String, Object> map2 = i.next();
            list.add((String) map2.get("feature.primaryKey"));
        }
        return list;
    }
}
