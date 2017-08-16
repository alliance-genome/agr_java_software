package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.searchableitem.DiseaseSearchableItemDocument;
import org.alliancegenome.indexer.document.searchableitem.SearchableItemDocument;
import org.alliancegenome.indexer.entity.DOTerm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiseaseToSearchableItemTranslator extends EntityDocumentTranslator<DOTerm, SearchableItemDocument> {
	
	private Logger log = LogManager.getLogger(getClass());
	
	@Override
	protected SearchableItemDocument entityToDocument(DOTerm entity) {
		
		log.info(entity);
		
		DiseaseSearchableItemDocument doc = new DiseaseSearchableItemDocument();

		doc.setDoDisplayId(entity.getDoDisplayId());
		doc.setDoId(entity.getDoId());
		doc.setDoPrefix(entity.getDoPrefix());
		doc.setDoUrl(entity.getDoUrl());
		doc.setId(entity.getPrimaryKey());
		
		doc.setCategory("disease");
		
		return doc;
	}

	@Override
	protected DOTerm doumentToEntity(SearchableItemDocument doument) {
		// TODO Auto-generated method stub
		return null;
	}

}
