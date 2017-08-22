package org.alliancegenome.indexer.translators;

import java.util.ArrayList;
import java.util.HashMap;

import org.alliancegenome.indexer.document.searchableitem.GeneSearchableItemDocument;
import org.alliancegenome.indexer.document.searchableitem.SearchableItemDocument;
import org.alliancegenome.indexer.entity.ExternalId;
import org.alliancegenome.indexer.entity.GOTerm;
import org.alliancegenome.indexer.entity.Gene;
import org.alliancegenome.indexer.entity.SecondaryId;
import org.alliancegenome.indexer.entity.Synonym;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneToSearchableItemTranslator extends EntityDocumentTranslator<Gene, SearchableItemDocument> {

	private Logger log = LogManager.getLogger(getClass());

	@Override
	protected SearchableItemDocument entityToDocument(Gene entity) {
		//log.info(entity);
		HashMap<String, ArrayList<String>> goTerms = new HashMap<>();

		GeneSearchableItemDocument s = new GeneSearchableItemDocument();

		s.setCategory("gene");

		s.setDataProvider(entity.getDataProvider());
		s.setDescription(entity.getDescription());
		
		if(entity.getSpecies() != null) {
			s.setSpecies(entity.getSpecies().getName());
		}

		ArrayList<String> external_ids = new ArrayList<>();
		if(entity.getExternalIds() != null) {
			for(ExternalId externalId: entity.getExternalIds()) {
				external_ids.add(externalId.getName());
			}
		}
		s.setExternal_ids(external_ids);

		// Setup Go Terms by type
		for(GOTerm term: entity.getGOTerms()) {
			ArrayList<String> list = goTerms.get(term.getType());
			if(list == null) {
				list = new ArrayList<>();
				goTerms.put(term.getType(), list);
			}
			if(!list.contains(term.getName())) {
				list.add(term.getName());
			}
		}
		
		//s.setDateProduced(entity.getDateProduced());

		s.setGene_biological_process(goTerms.get("biological_process"));
		s.setGene_cellular_component(goTerms.get("cellular_component"));
		s.setGene_molecular_function(goTerms.get("molecular_function"));

		s.setGeneLiteratureUrl(entity.getGeneLiteratureUrl());
		s.setGeneSynopsis(entity.getGeneSynopsis());
		s.setGeneSynopsisUrl(entity.getGeneSynopsisUrl());
		s.setGeneticEntityExternalUrl(entity.getGeneticEntityExternalUrl());

		s.setHref(null); // This might look wrong but it was taken from the old AGR code base.
		s.setName(entity.getName());
		s.setName_key(entity.getSymbol()); // This might look wrong but it was taken from the old AGR code base.
		s.setPrimaryId(entity.getPrimaryKey());
		if(entity.getCreatedBy() != null) {
			s.setRelease(entity.getCreatedBy().getRelease());
		}

		ArrayList<String> secondaryIds = new ArrayList<>();
		if(entity.getSecondaryIds() != null) {
			for(SecondaryId secondaryId: entity.getSecondaryIds()) {
				secondaryIds.add(secondaryId.getName());
			}		
		}
		s.setSecondaryIds(secondaryIds);

		if(entity.getSOTerm() != null) {
			s.setSoTermId(entity.getSOTerm().getPrimaryKey());
			s.setSoTermName(entity.getSOTerm().getName());
		}
		s.setSymbol(entity.getSymbol());

		ArrayList<String> synonyms = new ArrayList<>();
		if(entity.getSynonyms() != null) {
			for(Synonym synonym: entity.getSynonyms()) {
				if(synonym.getPrimaryKey() != null) {
					synonyms.add(synonym.getPrimaryKey());
				} else {
					synonyms.add(synonym.getName());
				}
			}
		}
		s.setSynonyms(synonyms);

		s.setTaxonId(entity.getTaxonId());
		
		// TODO s.setCrossReferences(crossReferences);
		// TODO s.setOrthology(orthology);
		// TODO s.setGenomeLocations(genomeLocations);
		// TODO s.setDiseases(diseases);
		
		
		
		
		//log.info(s);
		
//		try {
//			ObjectMapper mapper = new ObjectMapper();
//			log.info("JSON Entity: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(entity));
//			log.info("JSON Document: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(s));
//		} catch (JsonProcessingException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		return s;
	}

	@Override
	protected Gene documentToEntity(SearchableItemDocument document) {
		// We are not going to the database yet so will implement this when we need to
		return null;
	}

}
