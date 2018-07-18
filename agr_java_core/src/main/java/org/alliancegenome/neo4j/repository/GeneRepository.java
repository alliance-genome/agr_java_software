package org.alliancegenome.neo4j.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alliancegenome.neo4j.entity.node.Gene;
import org.neo4j.ogm.model.Result;

public class GeneRepository extends Neo4jRepository<Gene> {

    public GeneRepository() {
        super(Gene.class);
    }

    public Gene getOneGene(String primaryKey) {     
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";

        query += " MATCH p1=(q:Species)<-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH p5=(g)-[:ASSOCIATION]-(s:DiseaseEntityJoin)-[:ASSOCIATION]-(feature:Feature)";
        query += " OPTIONAL MATCH p2=(do:DOTerm)-[:ASSOCIATION]-(s:DiseaseEntityJoin)-[:EVIDENCE]->(ea)";
        query += " OPTIONAL MATCH p4=(g)-[:ASSOCIATION]-(s:OrthologyGeneJoin)-->(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]->(q2:Species), (s)-[:ASSOCIATION]->(g2)";
        query += " OPTIONAL MATCH p6=(g)-[:ASSOCIATION]->(s:PhenotypeEntityJoin)-[:ASSOCIATION]->(phenotype:Phenotype), p7=(g)--(s:PhenotypeEntityJoin)-[evidence:EVIDENCE]->(pub:Publication)";
        query += " OPTIONAL MATCH p8=(g)-[:ASSOCIATION]->(s:PhenotypeEntityJoin)<-[:ASSOCIATION]-(ff:Feature)";
        query += " OPTIONAL MATCH p9=(g:Gene)-[:ANNOTATED_TO]->(s:GOTerm)-[:IS_A|:PART_OF*]->(parent:GOTerm)";
        query += " RETURN p1, p2, p3, p4, p5, p6, p7, p8, p9";

        Iterable<Gene> genes = query(query, map);
        for(Gene g: genes) {
            if(g.getPrimaryKey().equals(primaryKey)) {
                return g;
            }
        }

        return null;
    }

    public HashMap<String, Gene> getGene(String primaryKey) {       
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";

        query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH p5=(g)--(s:DiseaseEntityJoin)--(feature:Feature)";
        query += " OPTIONAL MATCH p2=(do:DOTerm)--(s:DiseaseEntityJoin)-[:EVIDENCE]-(ea)";
        query += " OPTIONAL MATCH p4=(g)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]-(q2:Species), (s)--(g2)";
        query += " RETURN p1, p2, p3, p4, p5";

        HashMap<String, Gene> retMap = new HashMap<>();

        Iterable<Gene> genes = query(query, map);
        for(Gene g: genes) {
            retMap.put(g.getPrimaryKey(), g);
        }

        return retMap;
    }

    public List<String> getAllGeneKeys() {
        String query = "MATCH (g:Gene)-[:FROM_SPECIES]-(q:Species) RETURN distinct g.primaryKey";
        Result r = queryForResult(query);
        Iterator<Map<String, Object>> i = r.iterator();
        ArrayList<String> list = new ArrayList<>();

        while(i.hasNext()) {
            Map<String, Object> map2 = i.next();
            list.add((String)map2.get("g.primaryKey"));
        }
        return list;
    }

}
