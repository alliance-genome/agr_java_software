package org.alliancegenome.indexer.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.alliancegenome.indexer.entity.node.Gene;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.ogm.exception.MappingException;
import org.neo4j.ogm.model.Result;

public class GeneRepository extends Neo4jRepository<Gene> {
	
	private Logger log = LogManager.getLogger(getClass());
	
	public GeneRepository() {
		super(Gene.class);
	}
	
	public Gene getOneGene(String primaryKey) {		
		HashMap<String, String> map = new HashMap<String, String>();

		map.put("primaryKey", primaryKey);
		String query = "";
		query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey = {primaryKey}";
		query += " OPTIONAL MATCH p2=(do:DOTerm)--(s:DiseaseGeneJoin)-[:EVIDENCE]-(ea)";
		query += " OPTIONAL MATCH p4=(g)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]-(q2:Species), (s)--(g2)";
		query += " RETURN p1, p2, p3, p4";
		try {
			Iterable<Gene> genes = query(query, map);
			for(Gene g: genes) {
				if(g.getPrimaryKey().equals(primaryKey)) {
					return g;
				}
			}
		} catch (MappingException e) {
			log.info("MappingException: " + primaryKey);
			e.printStackTrace();
		}
		return null;
		
	}
	
	public Iterable<Gene> getGenes(List<String> ids) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("list", ids);
		String query = "";
		query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)--(s) WHERE g.primaryKey IN {list}";
		query += " OPTIONAL MATCH p2=(do:DOTerm)--(s:DiseaseGeneJoin)-[:EVIDENCE]-(ea)";
		query += " OPTIONAL MATCH p4=(g)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]-(q2:Species), (s)--(g2) WHERE g.primaryKey IN {list}";
		query += " RETURN p1, p2, p3, p4";
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
		query += "MATCH (g:Gene) WITH g SKIP " + (page * size) + " LIMIT " + size;
		query += " MATCH p1=(q:Species)-[:FROM_SPECIES]-(g)--(s)";
		query += " OPTIONAL MATCH p2=(do:DOTerm)--(s:DiseaseGeneJoin)-[:EVIDENCE]-(ea)";
		query += " OPTIONAL MATCH p4=(g)--(s:OrthologyGeneJoin)--(a:OrthoAlgorithm), p3=(g)-[o:ORTHOLOGOUS]-(g2:Gene)-[:FROM_SPECIES]-(q2:Species), (s)--(g2)";
		query += " RETURN p1, p2, p3, p4";
		return query(query);
	}

}
