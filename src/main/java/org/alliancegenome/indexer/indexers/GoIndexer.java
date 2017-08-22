package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.go.GoDocument;
import org.alliancegenome.indexer.entity.GOTerm;
import org.alliancegenome.indexer.service.Neo4jService;

public class GoIndexer extends Indexer<GoDocument> {

	private Neo4jService<GOTerm> neo4jService = new Neo4jService<GOTerm>(GOTerm.class);

	public GoIndexer(IndexerConfig config) {
		super(config);
	}
	
	public void index() {
		
	}

}
