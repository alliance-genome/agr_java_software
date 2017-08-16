package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.disease.DiseaseDocument;
import org.alliancegenome.indexer.entity.DOTerm;
import org.alliancegenome.indexer.service.Neo4jESService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiseaseIndexer extends Indexer<DiseaseDocument> {
	
	
	private Logger log = LogManager.getLogger(getClass());
	
	private Neo4jESService<DOTerm> neo4jService = new Neo4jESService<DOTerm>(DOTerm.class);

	public DiseaseIndexer(IndexerConfig config) {
		super(config);
	}
	
	@Override
	public void index() {
		
		
	}

}
