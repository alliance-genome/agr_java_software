package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.DiseaseDocument;
import org.alliancegenome.indexer.entity.Disease;
import org.alliancegenome.indexer.service.Neo4jESService;
import org.apache.log4j.Logger;

public class DiseaseIndexer extends Indexer<DiseaseDocument> {
	
	private Logger log = Logger.getLogger(getClass());
	
	private Neo4jESService<Disease, DiseaseDocument> neo4jService = new Neo4jESService<Disease, DiseaseDocument>();

	public DiseaseIndexer(IndexerConfig config) {
		super(config);
	}
	
	@Override
	public void index() {
		
		
	}

}
