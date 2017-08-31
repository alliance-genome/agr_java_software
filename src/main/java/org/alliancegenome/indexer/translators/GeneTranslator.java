package org.alliancegenome.indexer.translators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alliancegenome.indexer.document.CrossReferenceDocument;
import org.alliancegenome.indexer.document.GeneDocument;
import org.alliancegenome.indexer.document.GenomeLocationDocument;
import org.alliancegenome.indexer.document.OrthologyDocument;
import org.alliancegenome.indexer.entity.node.CrossReference;
import org.alliancegenome.indexer.entity.node.ExternalId;
import org.alliancegenome.indexer.entity.node.GOTerm;
import org.alliancegenome.indexer.entity.node.Gene;
import org.alliancegenome.indexer.entity.node.SecondaryId;
import org.alliancegenome.indexer.entity.node.Synonym;
import org.alliancegenome.indexer.entity.relationship.GenomeLocation;
import org.alliancegenome.indexer.entity.relationship.Orthologous;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeneTranslator extends EntityDocumentTranslator<Gene, GeneDocument> {

	private Logger log = LogManager.getLogger(getClass());

	//private static DiseaseTranslator diseaseTranslator = new DiseaseTranslator();

	@Override
	protected GeneDocument entityToDocument(Gene entity) {
		//log.info(entity);
		HashMap<String, ArrayList<String>> goTerms = new HashMap<>();

		GeneDocument geneDocument = new GeneDocument();

		geneDocument.setCategory("gene");

		geneDocument.setDataProvider(entity.getDataProvider());
		geneDocument.setDescription(entity.getDescription());

		geneDocument.setGeneLiteratureUrl(entity.getGeneLiteratureUrl());
		geneDocument.setGeneSynopsis(entity.getGeneSynopsis());
		geneDocument.setGeneSynopsisUrl(entity.getGeneSynopsisUrl());
		geneDocument.setGeneticEntityExternalUrl(entity.getGeneticEntityExternalUrl());

		geneDocument.setHref(null); // This might look wrong but it was taken from the old AGR code base.
		geneDocument.setName(entity.getName());
		geneDocument.setName_key(entity.getSymbol()); // This might look wrong but it was taken from the old AGR code base.
		geneDocument.setPrimaryId(entity.getPrimaryKey());
		//s.setDateProduced(entity.getDateProduced());
		geneDocument.setTaxonId(entity.getTaxonId());


		if(entity.getCreatedBy() != null) {
			geneDocument.setRelease(entity.getCreatedBy().getRelease());
		}
		if(entity.getSpecies() != null) {
			geneDocument.setSpecies(entity.getSpecies().getName());
		}


		ArrayList<String> external_ids = new ArrayList<>();
		if(entity.getExternalIds() != null) {
			for(ExternalId externalId: entity.getExternalIds()) {
				external_ids.add(externalId.getName());
			}
		}
		geneDocument.setExternal_ids(external_ids);


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
		geneDocument.setGene_biological_process(goTerms.get("biological_process"));
		geneDocument.setGene_cellular_component(goTerms.get("cellular_component"));
		geneDocument.setGene_molecular_function(goTerms.get("molecular_function"));

		ArrayList<String> secondaryIds = new ArrayList<>();
		if(entity.getSecondaryIds() != null) {
			for(SecondaryId secondaryId: entity.getSecondaryIds()) {
				secondaryIds.add(secondaryId.getName());
			}		
		}
		geneDocument.setSecondaryIds(secondaryIds);


		if(entity.getSOTerm() != null) {
			geneDocument.setSoTermId(entity.getSOTerm().getPrimaryKey());
			geneDocument.setSoTermName(entity.getSOTerm().getName());
		}
		geneDocument.setSymbol(entity.getSymbol());

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
		geneDocument.setSynonyms(synonyms);



		if(entity.getOrthoGenes() != null) {
			List<OrthologyDocument> olist = new ArrayList<>();
			for(Orthologous orth: entity.getOrthoGenes()) {
				OrthologyDocument doc = new OrthologyDocument(
						orth.getUuid(),
						orth.isBestScore(),
						orth.isBestRevScore(),
						orth.getConfidence(),
						orth.getGene1().getSpecies().getPrimaryId(),
						null, //orth.getGene2().getSpecies().getPrimaryId(),
						orth.getGene1().getSpecies().getName(),
						null, //orth.getGene2().getSpecies().getName(),
						orth.getGene2().getSymbol(),
						orth.getGene2().getPrimaryKey(),
						new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>()
						//predictionMethodsNotCalled, predictionMethodsMatched, predictionMethodsNotMatched
						);
				olist.add(doc);
			}
			geneDocument.setOrthology(olist);
		}

		//			if(entity.getDOTerms() != null) {
		//				List<DiseaseDocument> dlist = new ArrayList<>();
		//				for(DOTerm dot: entity.getDOTerms()) {
		//					DiseaseDocument doc = diseaseTranslator.translate(dot, true);
		//					dlist.add(doc);
		//				}
		//				geneDocument.setDiseases(dlist);
		//			}

		if(entity.getGenomeLocations() != null) {
			List<GenomeLocationDocument> gllist = new ArrayList<>();
			for(GenomeLocation location: entity.getGenomeLocations()) {
				GenomeLocationDocument loc = new GenomeLocationDocument(
						location.getStart(),
						location.getEnd(),
						location.getAssembly(),
						location.getStrand(),
						location.getChromosome().getPrimaryKey());

				gllist.add(loc);
			}
			geneDocument.setGenomeLocations(gllist);
		}

		if(entity.getCrossReferences() != null) {
			List<CrossReferenceDocument> crlist = new ArrayList<>();
			for(CrossReference cr: entity.getCrossReferences()) {
				CrossReferenceDocument crd = new CrossReferenceDocument(
						cr.getCrossrefCompleteUrl(),
						cr.getLocalId(),
						String.valueOf(cr.getId()),
						cr.getGlobalCrossrefId());
				crlist.add(crd);
			}
			geneDocument.setCrossReferences(crlist);
		}





		return geneDocument;
	}

	@Override
	protected Gene documentToEntity(GeneDocument document) {
		// We are not going to the database yet so will implement this when we need to
		return null;
	}

}
