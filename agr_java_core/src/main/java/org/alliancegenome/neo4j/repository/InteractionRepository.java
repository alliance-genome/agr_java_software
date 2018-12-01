package org.alliancegenome.neo4j.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

public class InteractionRepository extends Neo4jRepository<InteractionGeneJoin> {

    public InteractionRepository() {
        super(InteractionGeneJoin.class);
    }
    
    public List<InteractionGeneJoin> getInteractions(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();
        List<InteractionGeneJoin> ret = new ArrayList<>();
        
        map.put("primaryKey", primaryKey);
        String query = "MATCH p1=(g1:Gene)--(igj:InteractionGeneJoin)--(g2:Gene), p2=(igj:InteractionGeneJoin)--(s) where g1.primaryKey = {primaryKey}"
                + " OPTIONAL MATCH p3=(g1:Gene)-->(s1:Species) "
                + " OPTIONAL MATCH p4=(g2:Gene)-->(s2:Species) "
                + " RETURN p1, p2, p3, p4";
        //String query = "MATCH p1=(g:Gene)-[iw:INTERACTS_WITH]->(g2:Gene), p2=(g:Gene)-->(igj:InteractionGeneJoin)--(s) where g.primaryKey = {primaryKey} and iw.uuid = igj.primaryKey RETURN p1, p2";

        Iterable<InteractionGeneJoin> joins = query(query, map);
        for (InteractionGeneJoin join: joins) {
            ret.add(join);
        }

        return ret;
    }
}
