package org.alliancegenome.indexer.repository;

import org.alliancegenome.indexer.entity.Gene;

public class GeneRepository extends Neo4jRepository<Gene> {

	public GeneRepository() {
		super(Gene.class);
	}

}
