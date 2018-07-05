package org.alliancegenome.neo4j.repository;

import java.util.*;
import java.util.stream.Collectors;

import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.neo4j.ogm.model.Result;

public class GoRepository extends Neo4jRepository<GOTerm> {

    public GoRepository() {
        super(GOTerm.class);
    }

    public List<String> getAllGoKeys() {
        String query = "MATCH (g:GOTerm)-[:ANNOTATED_TO]-(ge:Gene)-[:FROM_SPECIES]-(s:Species) RETURN distinct g.primaryKey";
        Result r = queryForResult(query);
        Iterator<Map<String, Object>> i = r.iterator();

        ArrayList<String> list = new ArrayList<>();

        while(i.hasNext()) {
            Map<String, Object> map2 = i.next();
            list.add((String)map2.get("g.primaryKey"));
        }
        return list;
    }

    public GOTerm getOneGoTerm(String primaryKey) {
        HashMap<String, String> map = new HashMap<>();

        map.put("primaryKey", primaryKey);

        String query = "MATCH p0=(go:GOTerm)-[:ANNOTATED_TO]-(:Gene)-[:FROM_SPECIES]-(:Species) WHERE go.primaryKey = {primaryKey}" +
                " OPTIONAL MATCH p1=(go)-[:ALSO_KNOWN_AS]-(:Synonym)" +
                " OPTIONAL MATCH parents=(go)-[:IS_A*]->(parentTerm)";
        query += " RETURN p0, p1, parents";

        Iterable<GOTerm> gots = query(query, map);
        GOTerm term = null;
        Set<String> parentTermNames = new HashSet<>();
        for(GOTerm g: gots) {
            if(g.getPrimaryKey().equals(primaryKey)) {
                term = g;
            } else {
                parentTermNames.add(g.getName());
            }
        }

        term.setParentTermNames(parentTermNames.stream().collect(Collectors.toList()));

        return term;
    }

}
