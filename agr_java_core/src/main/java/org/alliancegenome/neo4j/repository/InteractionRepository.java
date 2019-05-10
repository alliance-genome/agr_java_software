package org.alliancegenome.neo4j.repository;

import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class InteractionRepository extends Neo4jRepository<InteractionGeneJoin> {

    public InteractionRepository() {
        super(InteractionGeneJoin.class);
    }

    private String interactionsQuery = "MATCH p1=(g1:Gene)--(igj:InteractionGeneJoin)--(g2:Gene), p2=(igj:InteractionGeneJoin)--(s) where g1.primaryKey = {primaryKey}"
            + " OPTIONAL MATCH p3=(g1:Gene)-->(s1:Species) "
            + " OPTIONAL MATCH p4=(g2:Gene)-->(s2:Species) ";

    public List<InteractionGeneJoin> getInteractions(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();
        List<InteractionGeneJoin> results = new ArrayList<>();

        map.put("primaryKey", primaryKey);
        String query = interactionsQuery + " RETURN p1, p2, p3, p4";
        //String query = "MATCH p1=(g:Gene)-[iw:INTERACTS_WITH]->(g2:Gene), p2=(g:Gene)-->(igj:InteractionGeneJoin)--(s) where g.primaryKey = {primaryKey} and iw.uuid = igj.primaryKey RETURN p1, p2";

        Iterable<InteractionGeneJoin> joins = query(query, map);
        for (InteractionGeneJoin join : joins) {
            results.add(join);
        }
        return results;
    }

    public long getInteractionCount(String geneID) {
        HashMap<String, String> bindingValueMap = new HashMap<>();
        bindingValueMap.put("primaryKey", geneID);

        String cypher = interactionsQuery + " RETURN count(distinct igj) as total";
        return (Long) queryForResult(cypher, bindingValueMap).iterator().next().get("total");
    }

    public long getInteractorCount(String geneID) {
        HashMap<String, String> bindingValueMap = new HashMap<>();
        bindingValueMap.put("primaryKey", geneID);

        String cypher = interactionsQuery + " RETURN count(distinct g2) as total";
        return (Long) queryForResult(cypher, bindingValueMap).iterator().next().get("total");
    }

    List<InteractionGeneJoin> getAllInteractions() {
        String allInteractionsQuery = "MATCH p1=(species1:Species)--(g1:Gene)--(igj:InteractionGeneJoin)--(g2:Gene)--(species2:Species), " +
                "p2=(igj:InteractionGeneJoin)-[:INTERACTOR_A_ROLE]->(mA:MITerm), "+
                "p3=(igj:InteractionGeneJoin)-[:INTERACTOR_B_ROLE]->(mB:MITerm), " +
                "p4=(igj:InteractionGeneJoin)-[:CROSS_REFERENCE]->(cross:CrossReference), " +
                "p5=(igj:InteractionGeneJoin)-[:DETECTION_METHOD]->(mde:MITerm), " +
                "p6=(igj:InteractionGeneJoin)-[:INTERACTOR_A_TYPE]->(typea:MITerm), " +
                "p7=(igj:InteractionGeneJoin)-[:INTERACTOR_B_TYPE]->(typeb:MITerm), " +
                "p8=(igj:InteractionGeneJoin)-[:INTERACTION_TYPE]->(type:MITerm), " +
                "p10=(igj:InteractionGeneJoin)-[:SOURCE_DATABASE]->(source:MITerm), " +
                "p11=(igj:InteractionGeneJoin)-[:AGGREGATION_DATABASE]->(aggregation:MITerm), " +
                "p9=(igj:InteractionGeneJoin)-[:EVIDENCE]->(pub:Publication) ";
//                " where g1.primaryKey = 'MGI:109583' ";
        String query = allInteractionsQuery + " RETURN p1, p2, p3, p5, p6, p7, p8, p4, p9, p10, p11 ";
        Iterable<InteractionGeneJoin> joins = query(query, new HashMap<>());
        return StreamSupport.stream(joins.spliterator(), false)
                .collect(Collectors.toList());
    }
}
