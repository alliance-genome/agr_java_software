package org.alliancegenome.shared.neo4j.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alliancegenome.shared.neo4j.entity.node.GOTerm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.exception.MappingException;
import org.neo4j.ogm.model.Result;

public class GoRepository extends Neo4jRepository<GOTerm> {

	private Logger log = LogManager.getLogger(getClass());

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
				" OPTIONAL MATCH p1=(go)-[:ALSO_KNOWN_AS]-(:Synonym)";
		query += " RETURN p0, p1";
		try {
			Iterable<GOTerm> gots = query(query, map);
			for(GOTerm g: gots) {
				if(g.getPrimaryKey().equals(primaryKey)) {
					return g;
				}
			}
		} catch (MappingException e) {
			log.error("Query: " + query);
			log.error("Map: " + map);
			e.printStackTrace();
		}

		return null;
	}

}
