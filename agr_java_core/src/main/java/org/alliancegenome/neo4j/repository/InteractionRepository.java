package org.alliancegenome.neo4j.repository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.entity.node.Species;
import org.neo4j.ogm.model.Result;

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

	public List<InteractionGeneJoin> getAllInteractions() {
		String query = "MATCH p1=(igj:InteractionGeneJoin)--(s) ";
		query +=  " RETURN p1";
		Iterable<InteractionGeneJoin> joins = query(query);
		return StreamSupport.stream(joins.spliterator(), false)
			.peek(this::populateSpeciesInfo)
			.collect(Collectors.toList());
	}
	
	public List<InteractionGeneJoin> getInteraction(String primaryKey) {
		String query = "MATCH p1=(igj:InteractionGeneJoin)--(s) WHERE igj.primaryKey = {primaryKey} ";
		query +=  " RETURN p1";
		
		HashMap<String, String> map = new HashMap<>();
		map.put("primaryKey", primaryKey);
		
		Iterable<InteractionGeneJoin> joins = query(query, map);
		return StreamSupport.stream(joins.spliterator(), false)
			.peek(this::populateSpeciesInfo)
			.collect(Collectors.toList());
	}
	
	public List<String> getAllInteractionJoinKeys() {
		String query = "MATCH (i:InteractionGeneJoin) RETURN i.primaryKey";

		Result r = queryForResult(query);
		Iterator<Map<String, Object>> i = r.iterator();

		ArrayList<String> list = new ArrayList<>();

		while (i.hasNext()) {
			Map<String, Object> map2 = i.next();
			list.add((String) map2.get("i.primaryKey"));
		}
		return list;
	}

	private void populateSpeciesInfo(InteractionGeneJoin join) {
		Gene geneA = join.getGeneA();
		geneA.setSpecies(Species.getSpeciesFromTaxonId(geneA.getTaxonId()));
		Gene geneB = join.getGeneB();
		geneB.setSpecies(Species.getSpeciesFromTaxonId(geneB.getTaxonId()));
	}

}
