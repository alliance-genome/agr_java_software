package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.entity.Gene;
import org.alliancegenome.indexer.service.GeneService;
import org.alliancegenome.indexer.translators.GeneTranslator;

public class GeneIndexer extends Indexer {

	private GeneService genesService = new GeneService();
	private GeneTranslator translate = new GeneTranslator();
	
	@Override
	public void run() {
		Iterable<Gene> genes = genesService.findAll();
		Iterable<GeneDocument> documents = translate.translateEntities(genes);
		genesService.create(documents);
	}
}