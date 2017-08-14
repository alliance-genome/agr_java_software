package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.SearchableItemDocument;
import org.alliancegenome.indexer.entity.GOTerm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GoToSearchableItemTranslator extends EntityDocumentTranslator<GOTerm, SearchableItemDocument> {
	
	private Logger log = LogManager.getLogger(getClass());
	
	@Override
	protected SearchableItemDocument entityToDocument(GOTerm entity) {
		log.info(entity);
		SearchableItemDocument doc = new SearchableItemDocument();
		doc.setPrimaryId(entity.getPrimaryKey());
		doc.setDescription(entity.getDescription());
		doc.setHref(entity.getHref());
		
		return doc;
	}

	@Override
	protected GOTerm doumentToEntity(SearchableItemDocument doument) {
		// TODO Auto-generated method stub
		return null;
	}

}
