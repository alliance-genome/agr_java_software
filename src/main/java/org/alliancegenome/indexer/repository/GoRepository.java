package org.alliancegenome.indexer.repository;

import org.alliancegenome.indexer.entity.GOTerm;

public class GoRepository extends Neo4jRepository<GOTerm> {

	public GoRepository() {
		super(GOTerm.class);
	}

}
