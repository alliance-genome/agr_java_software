package org.alliancegenome.indexer.service;

import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.entity.Gene;

public class GeneService extends Neo4jESService<Gene, GeneDocument> {

	public Class<Gene> getEntityType() {
		return Gene.class;
	}

}
