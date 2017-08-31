package org.alliancegenome.indexer.repository;

import org.alliancegenome.indexer.entity.node.Gene;

public class GeneRepository extends Neo4jRepository<Gene> {

	public GeneRepository() {
		super(Gene.class);
	}

	public Iterable<Gene> getGeneByPage(int i, int chunkSize) {
		
		return null;
	}

}
