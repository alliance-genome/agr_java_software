package org.alliancegenome.neo4j.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alliancegenome.neo4j.entity.node.Feature;
import org.neo4j.ogm.model.Result;

public class FeatureRepository extends Neo4jRepository<Feature> {

    public FeatureRepository() {
        super(Feature.class);
    }

    public Feature getFeature(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        String query = "";
        query += " MATCH p1=(feature:Feature)-[:IS_ALLELE_OF]-(g:Gene)-[:FROM_SPECIES]-(q:Species) WHERE feature.primaryKey = {primaryKey}";
        query += " OPTIONAL MATCH p2=(feature:Feature)-[:ASSOCIATION]-(diseaseJoin:DiseaseEntityJoin)-[:ASSOCIATION]-(do:DOTerm)";
        query += " OPTIONAL MATCH p3=(do:DOTerm)-[:ASSOCIATION]-(diseaseJoin:DiseaseEntityJoin)-[:EVIDENCE]-(ea)";
        query += " OPTIONAL MATCH p4=(feature:Feature)-[:ALSO_KNOWN_AS]-(synonym:Synonym)";
        query += " OPTIONAL MATCH p5=(feature:Feature)-[:ASSOCIATION]-(diseaseJoin:DiseaseEntityJoin)-[:ASSOCIATION]-(g:Gene)";
        query += " OPTIONAL MATCH p6=(feature:Feature)-[:HAS_PHENOTYPE]-(termName:Phenotype)";
        query += " OPTIONAL MATCH crossRef=(feature:Feature)-[:CROSS_REFERENCE]-(c:CrossReference)";
        query += " RETURN p1, p2, p3, p4, p5, p6, crossRef";
        
        Iterable<Feature> genes = query(query, map);
        for (Feature g : genes) {
            if (g.getPrimaryKey().equals(primaryKey)) {
                return g;
            }
        }

        return null;
    }


    public List<String> getAllFeatureKeys() {
        String query = "MATCH (feature:Feature)--(g:Gene)-[:FROM_SPECIES]-(q:Species) RETURN feature.primaryKey";

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
