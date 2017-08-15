package org.alliancegenome.indexer.translators;

import java.util.List;

import org.alliancegenome.indexer.document.SearchableItemDocument;
import org.alliancegenome.indexer.entity.GOTerm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GoToSearchableItemTranslator extends EntityDocumentTranslator<GOTerm, SearchableItemDocument> {
	
	private Logger log = LogManager.getLogger(getClass());
	
	@Override
	protected SearchableItemDocument entityToDocument(GOTerm entity) {
		//log.info(entity);
		SearchableItemDocument doc = new SearchableItemDocument();
		
//		private List<String> go_synonyms;
//		private List<String> go_genes;
//		private String name;
//		private List<String> go_species;
//		private String href;
//		private String id;
//		private String name_key;
//		private String go_type;
//		private String description;
		
		doc.setPrimaryId(entity.getPrimaryKey());
		doc.setDescription(entity.getDescription());
		doc.setHref(entity.getHref());
		doc.setName_key(entity.getNameKey());
		doc.setCategory("go");
		doc.setName(entity.getName());
		
		return doc;
	}

	@Override
	protected GOTerm doumentToEntity(SearchableItemDocument doument) {
		// TODO Auto-generated method stub
		return null;
	}

}
