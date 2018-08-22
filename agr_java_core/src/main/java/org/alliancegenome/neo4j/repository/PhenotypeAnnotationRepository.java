package org.alliancegenome.neo4j.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alliancegenome.neo4j.entity.node.PhenotypeEntityJoin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.model.Result;

public class PhenotypeAnnotationRepository extends Neo4jRepository<PhenotypeEntityJoin> {

    private Logger log = LogManager.getLogger(getClass());

    public PhenotypeAnnotationRepository() {
        super(PhenotypeEntityJoin.class);
    }

    public List<String> getAllPhenotypeEntityKeys() {
        String query = "MATCH (phenotypeEntity:PhenotypeEntityJoin) return phenotypeEntity.primaryKey";
        log.debug("Starting Query: " + query);
        Result r = queryForResult(query);
        Iterator<Map<String, Object>> i = r.iterator();

        ArrayList<String> list = new ArrayList<>();

        while (i.hasNext()) {
            Map<String, Object> map2 = i.next();
            list.add((String) map2.get("phenotypeEntity.primaryKey"));
        }
        log.debug("Query Finished: " + list.size());
        return list;
    }

    public PhenotypeEntityJoin PhenotypeEntityJoin(String primaryKey) {

        String cypher = "MATCH p0=(phenotype:Phenotype)--(phenotypeEntityJoin:PhenotypeEntityJoin)-[:EVIDENCE]-(publications:Publication)" +
                " WHERE phenotypeEntityJoin.primaryKey = {primaryKey}   " +
                " OPTIONAL MATCH p2=(phenotypeEntityJoin)--(g:Gene)-[:FROM_SPECIES]-(species:Species)" +
                " OPTIONAL MATCH p4=(phenotypeEntityJoin)--(feature:Feature)" +
                " RETURN p0, p2, p4";

        HashMap<String, String> map = new HashMap<>();
        map.put("primaryKey", primaryKey);

        PhenotypeEntityJoin phenotypeEntityJoin = null;

        Iterable<PhenotypeEntityJoin> terms = query(cypher, map);
        for (PhenotypeEntityJoin term : terms) {
            if (term.getPrimaryKey().equals(primaryKey)) {
                phenotypeEntityJoin = term;
            }
        }

        if (phenotypeEntityJoin == null) return null;
        return phenotypeEntityJoin;
    }

}
