package org.alliancegenome.neo4j.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

import com.google.common.collect.Iterators;

public class InteractionRepository extends Neo4jRepository<InteractionGeneJoin> {

    
    
    public InteractionRepository() {
        super(InteractionGeneJoin.class);
    }

    public List<InteractionGeneJoin> getInteractions(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);
        System.out.println(map);
        String query = "MATCH p1=(sp1:Species)-[:FROM_SPECIES]-(g1:Gene)-[iw:INTERACTS_WITH]-(g2:Gene)-[:FROM_SPECIES]-(sp2:Species), p2=(g1)-->(igj:InteractionGeneJoin)-->(g2) where (g1.primaryKey = {primaryKey} or g2.primaryKey = {primaryKey}) and iw.uuid = igj.primaryKey RETURN p1, p2";
        //String query = "MATCH p1=(g:Gene)-[iw:INTERACTS_WITH]->(g2:Gene), p2=(g:Gene)-->(igj:InteractionGeneJoin)--(s) where g.primaryKey = {primaryKey} and iw.uuid = igj.primaryKey RETURN p1, p2";

        Iterable<InteractionGeneJoin> joins = query(query, map);
        
        System.out.println("Count: " + Iterators.size(joins.iterator()));
        List<InteractionGeneJoin> ret = new ArrayList<>();
        //System.out.println(joins);
        for (InteractionGeneJoin join: joins) {
            System.out.println(join);
            //if (join.getPrimaryKey().equals(primaryKey)) {
            //  return join.getInteractions();
            //}
            ret.add(join);
        }
        return ret;
        //InteractionGeneJoin
    }
}
