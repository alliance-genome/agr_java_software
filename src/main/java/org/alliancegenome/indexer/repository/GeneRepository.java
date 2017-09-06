package org.alliancegenome.indexer.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alliancegenome.indexer.entity.node.Gene;
import org.neo4j.ogm.model.Result;

public class GeneRepository extends Neo4jRepository<Gene> {

	public GeneRepository() {
		super(Gene.class);
	}
	
	public Iterable<Gene> getOneGene(String primaryKey) {		
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("primaryKey", primaryKey);
		String query = "";
		query += "MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)-[]-(o) WHERE g.primaryKey = {primaryKey}";
		query += " OPTIONAL MATCH p3=(g:Gene)--(o:OrthologyGeneJoin)--(oa:OrthoAlgorithm)";
		query += " RETURN p1, p3";
		return query(query, map);
	}
	
	public List<String> getAllGeneIds() {
		String query = "MATCH (g:Gene) RETURN g.primaryKey";
		
		Result r = queryForResult(query);
		Iterator<Map<String, Object>> i = r.iterator();
		
		ArrayList<String> list = new ArrayList<String>();
		
		while(i.hasNext()) {
			Map<String, Object> map2 = i.next();
			list.add((String)map2.get("g.primaryKey"));
		}
		return list;
	}

	public Iterable<Gene> getGeneByPage(int page, int size) {
		String query = "";
		query += "MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)-[]-(o)";
		//query += " OPTIONAL MATCH p3=(g)--(o:OrthologyGeneJoin)--(oa:OrthoAlgorithm)";
		query += " RETURN p1";
		query += " SKIP " + (page * size) + " LIMIT " + size;
		return query(query);
	}

}
