package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.entity.Gene;
import org.alliancegenome.indexer.service.Neo4jService;
import org.alliancegenome.indexer.translators.GeneTranslator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneIndexer extends Indexer<GeneDocument> {

	private Logger log = LogManager.getLogger(getClass());
	private Neo4jService<Gene> geneNeo4jService = new Neo4jService<Gene>(Gene.class);
	private GeneTranslator geneTrans = new GeneTranslator();
	
	private GeneTranslator translate = new GeneTranslator();
	
	public GeneIndexer(String currnetIndex, TypeConfig config) {
		super(currnetIndex, config);
	}
	
	@Override
	public void index() {
		
		int geneCount = geneNeo4jService.getCount();
		int chunkSize = 100;
		int pages = geneCount / chunkSize;

		log.debug("GeneCount: " + geneCount);
		if(geneCount > 0) {
			startProcess(pages, chunkSize, geneCount);
			for(int i = 0; i <= pages; i++) {
				log.info("Before Neo");
				Iterable<Gene> gene_entities = geneNeo4jService.getPage(i, chunkSize);
				log.info("After Neo Before translate");
				addDocuments(geneTrans.translateEntities(gene_entities));
				log.info("After Add docs");
				progress(i, pages, chunkSize);
			}
			finishProcess(geneCount);
		}

	}

}