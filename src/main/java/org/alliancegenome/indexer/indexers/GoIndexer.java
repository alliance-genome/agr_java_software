package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.go.GoDocument;
import org.alliancegenome.indexer.entity.GOTerm;
import org.alliancegenome.indexer.service.Neo4jESService;

public class GoIndexer extends Indexer<GoDocument> {

	private Neo4jESService<GOTerm> neo4jService = new Neo4jESService<GOTerm>(GOTerm.class);

	public GoIndexer(IndexerConfig config) {
		super(config);
	}
	
	public void index() {
		
	}

}
