package org.alliancegenome.indexer.translators;

import java.util.ArrayList;
import java.util.HashMap;

import org.alliancegenome.indexer.document.SearchableItemDocument;
import org.alliancegenome.indexer.entity.GOTerm;
import org.alliancegenome.indexer.entity.Gene;
import org.alliancegenome.indexer.entity.Synonym;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneToSearchableItemTranslator extends EntityDocumentTranslator<Gene, SearchableItemDocument> {
	
	private Logger log = LogManager.getLogger(getClass());
	
	@Override
	protected SearchableItemDocument entityToDocument(Gene entity) {
		log.info(entity);
		HashMap<String, ArrayList<String>> goTerms = new HashMap<String, ArrayList<String>>();
		
		SearchableItemDocument s = new SearchableItemDocument();
		
		s.setCategory("gene");
		//s.setCrossReferences(crossReferences);
		s.setDataProvider(entity.getDataProvider());
		s.setDescription(entity.getDescription());
		//s.setDiseases(diseases);
		//s.setExternal_ids(external_ids);
		
		
		for(GOTerm term: entity.getGOTerms()) {
			ArrayList<String> list = goTerms.get(term.getType());
			if(list == null) {
				list = new ArrayList<String>();
				goTerms.put(term.getType(), list);
			}
			if(!list.contains(term.getName())) {
				list.add(term.getName());
			}
		}
		
		s.setGene_biological_process(goTerms.get("biological_process"));
		s.setGene_cellular_component(goTerms.get("cellular_component"));
		s.setGene_molecular_function(goTerms.get("molecular_function"));
		
		s.setGeneLiteratureUrl(entity.getGeneLiteratureUrl());
		s.setGeneSynopsis(entity.getGeneSynopsis());
		s.setGeneSynopsisUrl(entity.getGeneSynopsisUrl());
		
		//s.setGenomeLocations(genomeLocations);
		//s.setHref(href);
		//s.setModCrossReference(modCrossReference);
		s.setName(entity.getName());
		//s.setName_key(name_key);
		//s.setOrthology(orthology);
		s.setPrimaryId(entity.getPrimaryKey());
		//s.setRelease(release);
		//s.setSecondaryIds(secondaryIds);
		if(entity.getSOTerm() != null) {
			s.setSoTermId(entity.getSOTerm().getPrimaryKey());
			s.setSoTermName(entity.getSOTerm().getName());
		}
		s.setSymbol(entity.getSymbol());
		
		ArrayList<String> synonyms = new ArrayList<String>();
		for(Synonym synonym: entity.getSynonyms()) {
			synonyms.add(synonym.getName());
		}
		s.setSynonyms(synonyms);

		s.setTaxonId(entity.getTaxonId());
		log.info(s);
		return s;
	}

	@Override
	protected Gene doumentToEntity(SearchableItemDocument doument) {
		// We are not going to the database yet so will implement this when we need to
		return null;
	}

}
