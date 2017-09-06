package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.entity.node.Gene;
import org.alliancegenome.indexer.repository.GeneRepository;
import org.alliancegenome.indexer.translators.GeneTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneIndexer extends Indexer<GeneDocument> {

	private Logger log = LogManager.getLogger(getClass());
	private GeneRepository repo = new GeneRepository();
	private GeneTranslator geneTrans = new GeneTranslator();

	public GeneIndexer(String currnetIndex, TypeConfig config) {
		super(currnetIndex, config);
	}
	
	@Override
	public void index() {
		
		int geneCount = repo.getCount();
		int chunkSize = typeConfig.getFetchChunkSize();
		int pages = geneCount / chunkSize;

		log.debug("GeneCount: " + geneCount);
		if(geneCount > 0) {
			startProcess(pages, chunkSize, geneCount);
			// Number 70806
			for(int i = 0; i <= pages; i++) {
				Iterable<Gene> gene_entities = repo.getGeneByPage(i, chunkSize);
				addDocuments(geneTrans.translateEntities(gene_entities));
				progress(i, pages, chunkSize);
			}
			finishProcess(geneCount);
		}

	}

}