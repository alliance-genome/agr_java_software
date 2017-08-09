package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.entity.Gene;
import org.alliancegenome.indexer.service.Neo4jESService;
import org.alliancegenome.indexer.translators.GeneTranslator;
import org.apache.log4j.Logger;

public class GeneIndexer extends Indexer<GeneDocument> {

	private Logger log = Logger.getLogger(getClass());
	
	
	private Neo4jESService<Gene, GeneDocument> neo4jService = new Neo4jESService<Gene, GeneDocument>();

	private GeneTranslator translate = new GeneTranslator();
	
	public GeneIndexer(IndexerConfig config) {
		super(config);
	}
	
	@Override
	public void index() {


	}

}