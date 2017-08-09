package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.SearchableItemDocument;
import org.alliancegenome.indexer.entity.Gene;

public class GeneToSearchableItemTranslator extends EntityDocumentTranslator<Gene, SearchableItemDocument> {

	@Override
	protected SearchableItemDocument entityToDocument(Gene entity) {
		
		return null;
	}

	@Override
	protected Gene doumentToEntity(SearchableItemDocument doument) {
		
		return null;
	}

}
