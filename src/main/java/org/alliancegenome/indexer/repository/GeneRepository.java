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
		query += "MATCH p1=(g:Gene)--(s) WHERE g.primaryKey = {primaryKey}";
		query += " OPTIONAL MATCH p2=(do:DOTerm)--(s:DiseaseGeneJoin)-[:EVIDENCE]-(q)";
		query += " OPTIONAL MATCH p3=(s)--(oa:OrthoAlgorithm)";
		query += " RETURN p1, p2, p3";
		return query(query, map);
	}
	
	public List<String> getAllGeneKeys() {
		String query = "MATCH (q:Species)-[:FROM_SPECIES]-(g:Gene) RETURN g.primaryKey";
		
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
		query += "MATCH p1=(g:Gene)--(s)";
		query += " OPTIONAL MATCH p2=(do:DOTerm)--(s:DiseaseGeneJoin)-[:EVIDENCE]-(q)";
		query += " OPTIONAL MATCH p4=(g)--(j:OrthoGeneJoin)--(s), p5=(j)--(a:OrthoAlgorithm)";
		query += " RETURN p1, p2, p4, p5";
		//query += " OPTIONAL MATCH p3=(s)--(oa:OrthoAlgorithm)";
		//query += " RETURN p1, p2, p3";
		query += " SKIP " + (page * size) + " LIMIT " + size;


		return query(query);
	}

}
