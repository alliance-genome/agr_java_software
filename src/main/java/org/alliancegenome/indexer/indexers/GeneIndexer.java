package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.entity.Gene;
import org.alliancegenome.indexer.service.Neo4jESService;
import org.alliancegenome.indexer.translators.GeneTranslator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneIndexer extends Indexer<GeneDocument> {

	private Logger log = LogManager.getLogger(getClass());
	
	
	private Neo4jESService<Gene> neo4jService = new Neo4jESService<Gene>(Gene.class);

	private GeneTranslator translate = new GeneTranslator();
	
	public GeneIndexer(IndexerConfig config) {
		super(config);
	}
	
	@Override
	public void index() {


	}

}