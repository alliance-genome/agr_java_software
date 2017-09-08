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
		query += "MATCH (g:Gene) WHERE g.primaryKey = {primaryKey}";
		query += " WITH g";
		query += " OPTIONAL MATCH p2=(g)--(s)";
		query += " WITH g, p2";
		query += " OPTIONAL MATCH p3=(g)--(m:OrtholgyGeneJoin)--(oa:OrthoAlgorithm)";
		query += " WITH g, p2, p3";
		query += " OPTIONAL MATCH p4=(g)--(m:DiseaseGeneJoin)-[:EVIDENCE]-(q)";
		//query += " p6=(g)-[:IS_IMPLICATED_IN]-(do:DOTerm),";
		//query += " p7=(g)--(m:OrtholgyGeneJoin)--(oa:OrthoAlgorithm)";
		query += " RETURN g, p2, p3, p4";
		
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
//		query += "MATCH p1=(q:Species)-[:FROM_SPECIES]-(g:Gene)-[]-(m)";
//		query += " WITH p1, m";
//		query += " OPTIONAL MATCH p3=(q:Species)-[:FROM_SPECIES]-(m:Gene)--(oa:OrthoAlgorithm)";
//		query += " RETURN p1, p3";
//		query += " SKIP " + (page * size) + " LIMIT " + size;

		query += "MATCH (g:Gene) ";
		query += " WITH g SKIP " + (page * size) + " LIMIT " + size;
		query += " OPTIONAL MATCH p2=(g)--(s)";
		query += " WITH g, p2";
		query += " OPTIONAL MATCH p3=(g)--(m:OrtholgyGeneJoin)--(oa:OrthoAlgorithm)";
		query += " WITH g, p2, p3";
		query += " OPTIONAL MATCH p4=(g)--(m:DiseaseGeneJoin)-[:EVIDENCE]-(q)";
		//query += " p6=(g)-[:IS_IMPLICATED_IN]-(do:DOTerm),";
		//query += " p7=(g)--(m:OrtholgyGeneJoin)--(oa:OrthoAlgorithm)";
		query += " RETURN g, p2, p3, p4";
		
//		query += "MATCH (g:Gene) WITH g";
//		//query += " OPTIONAL MATCH p3=(q:Species)-[:FROM_SPECIES]-(m:Gene)--(oa:OrthoAlgorithm)";
//		query += " OPTIONAL MATCH ";
//		//query += " p2=(g)--(s:Synonym),";
//		//query += " p3=(g)--(e:ExternalId),";
//		//query += " p4=(g)--(se:SecondaryId),";
//		query += " p9=(g)--(m:DiseaseGeneJoin)--(p:Publication),";
//		//query += " p10=(g)--(m)--(e:Evidence)";
//		query += " RETURN p1, p2, p3";
//		query += " SKIP " + (page * size) + " LIMIT " + size;
		return query(query);
	}

}
