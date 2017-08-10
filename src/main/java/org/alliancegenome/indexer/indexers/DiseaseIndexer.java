package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.DiseaseDocument;
import org.alliancegenome.indexer.entity.Disease;
import org.alliancegenome.indexer.service.Neo4jESService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiseaseIndexer extends Indexer<DiseaseDocument> {
	
	
	private Logger log = LogManager.getLogger(getClass());
	
	private Neo4jESService<Disease> neo4jService = new Neo4jESService<Disease>(Disease.class);

	public DiseaseIndexer(IndexerConfig config) {
		super(config);
	}
	
	@Override
	public void index() {
		
		
	}

}
