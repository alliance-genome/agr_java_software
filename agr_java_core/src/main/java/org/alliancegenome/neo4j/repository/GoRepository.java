package org.alliancegenome.neo4j.repository;

import java.util.*;

import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.neo4j.ogm.model.Result;

public class GoRepository extends Neo4jRepository<GOTerm> {

    public GoRepository() {
        super(GOTerm.class);
    }

    public List<String> getAllGoKeys() {
        String query = "MATCH (g:GOTerm) RETURN distinct g.primaryKey";
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

        String query = "MATCH p0=(go:GOTerm) WHERE go.primaryKey = {primaryKey}" +
                " OPTIONAL MATCH pSyn=(go)-[:ALSO_KNOWN_AS]-(:Synonym)";
        query += " RETURN p0, pSyn";

        Iterable<GOTerm> gots = query(query, map);
        for(GOTerm g: gots) {
            if(g.getPrimaryKey().equals(primaryKey)) {
                return g;
            }
        }

        return null;
    }


    public Map<String, Set<String>> getGoTermToGeneMap() {

        Map<String,Set<String>> map = new HashMap<>();

        String query = "MATCH (go:GOTerm)--(gene:Gene)--(species:Species) RETURN go.primaryKey,gene.symbol,species.name";
        Result r = queryForResult(query);
        Iterator<Map<String, Object>> i = r.iterator();

        while (i.hasNext()) {
            Map<String, Object> resultMap = i.next();
            String primaryKey = resultMap.get("go.primaryKey").toString();
            String geneSymbol = resultMap.get("gene.symbol").toString();
            String speciesName = resultMap.get("species.name").toString();

            SpeciesType speciesType = SpeciesType.getTypeByName(speciesName);
            String nameKey = geneSymbol + "(" + speciesType.getAbbreviation() + ")";

            if (map.get(primaryKey) == null) {
                map.put(primaryKey,new HashSet<>());
            }

            map.get(primaryKey).add(nameKey);
        }

        return map;
    }

}
