package org.alliancegenome.indexer.translators;

import java.util.ArrayList;

import org.alliancegenome.indexer.document.searchableitem.GoSearchableItemDocument;
import org.alliancegenome.indexer.document.searchableitem.SearchableItemDocument;
import org.alliancegenome.indexer.entity.GOTerm;
import org.alliancegenome.indexer.entity.Gene;
import org.alliancegenome.indexer.entity.Synonym;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GoToSearchableItemTranslator extends EntityDocumentTranslator<GOTerm, SearchableItemDocument> {
	
	private Logger log = LogManager.getLogger(getClass());
	
	@Override
	protected SearchableItemDocument entityToDocument(GOTerm entity) {
		log.info(entity);
		GoSearchableItemDocument doc = new GoSearchableItemDocument();
		
		// TODO private List<String> go_species;
		
		doc.setName(entity.getName());
		doc.setHref(entity.getHref());
		doc.setId(entity.getPrimaryKey());
		doc.setName_key(entity.getNameKey());
		doc.setGo_type(entity.getType());
		doc.setDescription(entity.getDescription());
		doc.setCategory("go");
		

		ArrayList<String> go_synonyms = new ArrayList<String>();
		//log.info(entity.getGoSynonyms());
		for(Synonym s: entity.getSynonyms()) {
			go_synonyms.add(s.getPrimaryKey());
		}
		doc.setGo_genes(go_synonyms);
		
//		ArrayList<String> go_genes = new ArrayList<String>();
//		log.info(entity.getGenes());
//		for(Gene g: entity.getGenes()) {
//			log.info(g.getId());
//			go_genes.add(g.getSymbol());
//		}
//		log.info(go_genes.size());
//		doc.setGo_genes(go_genes);
		
		return doc;
	}

	@Override
	protected GOTerm doumentToEntity(SearchableItemDocument doument) {
		// TODO Auto-generated method stub
		return null;
	}

}
