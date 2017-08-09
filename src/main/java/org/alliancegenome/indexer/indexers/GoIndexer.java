package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.GoDocument;
import org.alliancegenome.indexer.entity.Go;
import org.alliancegenome.indexer.service.Neo4jESService;

public class GoIndexer extends Indexer {

	private Neo4jESService<Go, GoDocument> neo4jService = new Neo4jESService<Go, GoDocument>();


	public GoIndexer(IndexerConfig config) {
		super(config);
	}
	
	public void index() {
		
	}

}
