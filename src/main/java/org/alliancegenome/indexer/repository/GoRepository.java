package org.alliancegenome.indexer.repository;

import org.alliancegenome.indexer.entity.node.GOTerm;

public class GoRepository extends Neo4jRepository<GOTerm> {

	public GoRepository() {
		super(GOTerm.class);
	}
	
	public Iterable<GOTerm> getGOTermsByPage(int page, int size) {
		String query = "MATCH (go:GOTerm) WITH go SKIP " + (page * size) + " LIMIT " + size;
		query += " MATCH p1=(go)-[:ANNOTATED_TO]-(:Gene)-[:FROM_SPECIES]-(:Species)";
		query += " MATCH p2=(go)-[:ALSO_KNOWN_AS]-(:Synonym)";
		query += " RETURN go, p1, p2";
		return query(query);
	}

}
