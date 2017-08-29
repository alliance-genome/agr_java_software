package org.alliancegenome.indexer.repository;

import org.alliancegenome.indexer.entity.DOTerm;

public class DiseaseRepository extends Neo4jRepository<DOTerm>{

	public DiseaseRepository() {
		super(DOTerm.class);

	}

}
