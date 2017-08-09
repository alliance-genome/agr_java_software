package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.indexer.document.DiseaseDocument;
import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.document.GoDocument;
import org.alliancegenome.indexer.entity.Disease;
import org.alliancegenome.indexer.entity.Gene;
import org.alliancegenome.indexer.entity.Go;
import org.alliancegenome.indexer.service.Neo4jESService;

public class SearchableItemIndexer extends Indexer {

	private Neo4jESService<Gene, GeneDocument> geneNeo4jService = new Neo4jESService<Gene, GeneDocument>();
	private Neo4jESService<Disease, DiseaseDocument> diseaseNeo4jService = new Neo4jESService<Disease, DiseaseDocument>();
	private Neo4jESService<Go, GoDocument> goNeo4jService = new Neo4jESService<Go, GoDocument>();
	
	public SearchableItemIndexer(IndexerConfig config) {
		super(config);
	}
	
	@Override
	public void index() {
		
		//Iterable<Gene> genes = neo4jService.findAll();
		//Iterable<GeneDocument> documents = translate.translateEntities(genes);
		//neo4jService.create(documents);
	}

}
