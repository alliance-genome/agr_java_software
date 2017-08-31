package org.alliancegenome.indexer.repository;

import org.alliancegenome.indexer.entity.node.Gene;

public class GeneRepository extends Neo4jRepository<Gene> {

	public GeneRepository() {
		super(Gene.class);
	}

	public Iterable<Gene> getGeneByPage(int page, int size) {
		String query = "MATCH p=(g:Gene)-[]-() SKIP " + (page * size) + " LIMIT " + size;
		query += " RETURN p";
		return query(query);
	}

}
