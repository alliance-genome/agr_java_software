package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.GoDocument;
import org.alliancegenome.indexer.entity.GoTerm;
import org.alliancegenome.indexer.service.Neo4jESService;

public class GoIndexer extends Indexer<GoDocument> {

	private Neo4jESService<GoTerm> neo4jService = new Neo4jESService<GoTerm>(GoTerm.class);

	public GoIndexer(IndexerConfig config) {
		super(config);
	}
	
	public void index() {
		
	}

}
