package org.alliancegenome.neo4j.repository;

import org.alliancegenome.neo4j.entity.node.Phenotype;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.model.Result;

import java.util.*;

public class PhenotypeRepository extends Neo4jRepository<Phenotype> {

    private Logger log = LogManager.getLogger(getClass());

    public PhenotypeRepository() {
        super(Phenotype.class);
    }

    public List<String> getAllPhenotypeKeys() {
        String query = "MATCH (termName:Phenotype) return termName.primaryKey";
        log.debug("Starting Query: " + query);
        Result r = queryForResult(query);
        Iterator<Map<String, Object>> i = r.iterator();

        ArrayList<String> list = new ArrayList<>();

        while (i.hasNext()) {
            Map<String, Object> map2 = i.next();
            list.add((String) map2.get("termName.primaryKey"));
        }
        log.debug("Query Finished: " + list.size());
        return list;
    }

    public Phenotype getPhenotypeTerm(String primaryKey) {

        String cypher = "MATCH p0=(termName:Phenotype)--(phenotypeEntityJoin:PhenotypeEntityJoin)-[:EVIDENCE]-(publications:Publication)" +
                " WHERE termName.primaryKey = {primaryKey}   " +
                " OPTIONAL MATCH p2=(phenotypeEntityJoin)--(g:Gene)-[:FROM_SPECIES]-(species:Species)" +
                " OPTIONAL MATCH p4=(phenotypeEntityJoin)--(feature:Feature)" +
                " OPTIONAL MATCH crossRefMatch=(phenotypeEntityJoin)--(feature:Feature)--(crossRef:CrossReference)" +
                " RETURN p0, p2, p4, crossRefMatch ";

        HashMap<String, String> map = new HashMap<>();
        map.put("primaryKey", primaryKey);

        Phenotype primaryTerm = null;

        Iterable<Phenotype> terms = query(cypher, map);
        for (Phenotype term : terms) {
            if (term.getPrimaryKey().equals(primaryKey)) {
                primaryTerm = term;
            }
        }

        if (primaryTerm == null) return null;
        return primaryTerm;
    }
    
}
