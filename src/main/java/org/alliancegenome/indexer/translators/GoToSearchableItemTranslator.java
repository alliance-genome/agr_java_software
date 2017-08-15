package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.searchableitem.GoSearchableItemDocument;
import org.alliancegenome.indexer.document.searchableitem.SearchableItemDocument;
import org.alliancegenome.indexer.entity.GOTerm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GoToSearchableItemTranslator extends EntityDocumentTranslator<GOTerm, SearchableItemDocument> {
	
	private Logger log = LogManager.getLogger(getClass());
	
	@Override
	protected SearchableItemDocument entityToDocument(GOTerm entity) {
		//log.info(entity);
		GoSearchableItemDocument doc = new GoSearchableItemDocument();
		
//		private List<String> go_synonyms;
//		private List<String> go_genes;
//		private List<String> go_species;
		
		doc.setName(entity.getName());
		doc.setHref(entity.getHref());
		doc.setId(entity.getPrimaryKey());
		doc.setName_key(entity.getNameKey());
		doc.setGo_type(entity.getType());
		doc.setDescription(entity.getDescription());
		doc.setCategory("go");
		
		
		
		
		return doc;
	}

	@Override
	protected GOTerm doumentToEntity(SearchableItemDocument doument) {
		// TODO Auto-generated method stub
		return null;
	}

}
