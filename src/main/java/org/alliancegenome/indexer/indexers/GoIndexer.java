package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.GoDocument;
import org.alliancegenome.indexer.entity.GOTerm;
import org.alliancegenome.indexer.repository.GoRepository;
import org.alliancegenome.indexer.translators.GoTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GoIndexer extends Indexer<GoDocument> {

	private Logger log = LogManager.getLogger(getClass());

	private GoRepository repo = new GoRepository();
	private GoTranslator goTrans = new GoTranslator();

	
	public GoIndexer(String currnetIndex, TypeConfig config) {
		super(currnetIndex, config);
	}
	
	public void index() {
		
		int goCount = repo.getCount();
		int chunkSize = typeConfig.getFetchChunkSize();
		int pages = goCount / chunkSize;

		log.debug("GoCount: " + goCount);
		if(goCount > 0) {
			startProcess(pages, chunkSize, goCount);
			for(int i = 0; i <= pages; i++) {
				Iterable<GOTerm> go_entities = repo.getPage(i, chunkSize);
				addDocuments(goTrans.translateEntities(go_entities));
				progress(i, pages, chunkSize);
			}
			finishProcess(goCount);
		}
	}

}
