package org.alliancegenome.indexer.indexers;

import org.alliancegenome.indexer.config.TypeConfig;
import org.alliancegenome.indexer.document.GoDocument;
import org.alliancegenome.indexer.entity.GOTerm;
import org.alliancegenome.indexer.service.Neo4jService;
import org.alliancegenome.indexer.translators.GoTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GoIndexer extends Indexer<GoDocument> {

	private Logger log = LogManager.getLogger(getClass());
	private Neo4jService<GOTerm> goNeo4jService = new Neo4jService<GOTerm>(GOTerm.class);
	private GoTranslator goTrans = new GoTranslator();

	
	public GoIndexer(String currnetIndex, TypeConfig config) {
		super(currnetIndex, config);
	}
	
	public void index() {
		
		
		int goCount = goNeo4jService.getCount();
		int chunkSize = 500;
		int pages = goCount / chunkSize;

		log.debug("GoCount: " + goCount);
		if(goCount > 0) {
			startProcess(pages, chunkSize, goCount);
			for(int i = 0; i <= pages; i++) {
				Iterable<GOTerm> go_entities = goNeo4jService.getPage(i, chunkSize);
				addDocuments(goTrans.translateEntities(go_entities));
				progress(i, pages, chunkSize);
			}
			finishProcess(goCount);
		}
	}

}
