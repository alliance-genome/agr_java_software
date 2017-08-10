package org.alliancegenome.indexer.translators;

import java.util.ArrayList;

import org.alliancegenome.indexer.document.SearchableItemDocument;
import org.alliancegenome.indexer.entity.Gene;
import org.alliancegenome.indexer.entity.Synonym;

public class GeneToSearchableItemTranslator extends EntityDocumentTranslator<Gene, SearchableItemDocument> {

	@Override
	protected SearchableItemDocument entityToDocument(Gene entity) {
		SearchableItemDocument s = new SearchableItemDocument();
		
		s.setCategory("gene");
		//s.setCrossReferences(crossReferences);
		s.setDataProvider(entity.getDataProvider());
		s.setDescription(entity.getDescription());
		//s.setDiseases(diseases);
		//s.setExternal_ids(external_ids);
		//s.setGene_biological_process(gene_biological_process);
		//s.setGene_cellular_component(gene_cellular_component);
		//s.setGene_molecular_function(gene_molecular_function);
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
		if(entity.getSoTerm() != null) {
			s.setSoTermId(entity.getSoTerm().getPrimaryKey());
			s.setSoTermName(entity.getSoTerm().getName());
		}
		s.setSymbol(entity.getSymbol());
		ArrayList<String> synonyms = new ArrayList<String>();
		for(Synonym synonym: entity.getSynonyms()) {
			synonyms.add(synonym.getName());
		}
		s.setSynonyms(synonyms);

		s.setTaxonId(entity.getTaxonId());
		return s;
	}

	@Override
	protected Gene doumentToEntity(SearchableItemDocument doument) {
		// We are not going to the database yet so will implement this when we need to
		return null;
	}

}
