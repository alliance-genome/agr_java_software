package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.*;
import org.alliancegenome.indexer.entity.node.*;
import org.alliancegenome.indexer.entity.relationship.GenomeLocation;
import org.alliancegenome.indexer.entity.relationship.Orthologous;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class GeneTranslator extends EntityDocumentTranslator<Gene, GeneDocument> {

    private Logger log = LogManager.getLogger(getClass());

    private static DiseaseTranslator diseaseTranslator = new DiseaseTranslator();

    @Override
    protected GeneDocument entityToDocument(Gene entity, int translationDepth) {
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

        geneDocument.setModCrossRefCompleteUrl(entity.getModCrossRefCompleteUrl());
        geneDocument.setModLocalId(entity.getModLocalId());
        geneDocument.setModGlobalCrossRefId(entity.getModGlobalCrossRefId());
        geneDocument.setModGlobalId(entity.getModGlobalId());

        geneDocument.setHref(null); // This might look wrong but it was taken from the old AGR code base.
        geneDocument.setName(entity.getName());
        geneDocument.setName_key(entity.getSymbol()); // This might look wrong but it was taken from the old AGR code base.
        geneDocument.setPrimaryId(entity.getPrimaryKey());
        //s.setDateProduced(entity.getDateProduced());
        geneDocument.setTaxonId(entity.getTaxonId());


        if (entity.getCreatedBy() != null) {
            geneDocument.setRelease(entity.getCreatedBy().getRelease());
        }
        if (entity.getSpecies() != null) {
            geneDocument.setSpecies(entity.getSpecies().getName());
        }

        // Setup Go Terms by type
        for (GOTerm term : entity.getGOTerms()) {
            ArrayList<String> list = goTerms.get(term.getType());
            if (list == null) {
                list = new ArrayList<>();
                goTerms.put(term.getType(), list);
            }
            if (!list.contains(term.getName())) {
                list.add(term.getName());
            }
        }
        geneDocument.setGene_biological_process(goTerms.get("biological_process"));
        geneDocument.setGene_cellular_component(goTerms.get("cellular_component"));
        geneDocument.setGene_molecular_function(goTerms.get("molecular_function"));

        ArrayList<String> secondaryIds = new ArrayList<>();
        if (entity.getSecondaryIds() != null) {
            for (SecondaryId secondaryId : entity.getSecondaryIds()) {
                secondaryIds.add(secondaryId.getName());
            }
        }
        geneDocument.setSecondaryIds(secondaryIds);


        if (entity.getSOTerm() != null) {
            geneDocument.setSoTermId(entity.getSOTerm().getPrimaryKey());
            geneDocument.setSoTermName(entity.getSOTerm().getName());
        }
        geneDocument.setSymbol(entity.getSymbol());

        ArrayList<String> synonyms = new ArrayList<>();
        if (entity.getSynonyms() != null) {
            for (Synonym synonym : entity.getSynonyms()) {
                if (synonym.getPrimaryKey() != null) {
                    synonyms.add(synonym.getPrimaryKey());
                } else {
                    synonyms.add(synonym.getName());
                }
            }
        }
        geneDocument.setSynonyms(synonyms);


//		if(entity.getOrthoGenes() != null) {
//		if(lookup.size() + entity.getOrthologyGeneJoins().size() > 0) {
//			System.out.println(lookup.size() + " ==? " + entity.getOrthologyGeneJoins().size());
//		}

        if (entity.getOrthologyGeneJoins().size() > 0) {
            List<OrthologyDoclet> olist = new ArrayList<>();

            HashMap<String, Orthologous> lookup = new HashMap<String, Orthologous>();
            for (Orthologous o : entity.getOrthoGenes()) {
                lookup.put(o.getPrimaryKey(), o);
            }

            for (OrthologyGeneJoin join : entity.getOrthologyGeneJoins()) {

                if (lookup.containsKey(join.getPrimaryKey())) {

                    ArrayList<String> matched = new ArrayList<String>();
                    if (join != null && join.getMatched() != null) {
                        for (OrthoAlgorithm algo : join.getMatched()) {
                            matched.add(algo.getName());
                        }
                    }
                    ArrayList<String> notMatched = new ArrayList<String>();
                    if (join != null && join.getNotMatched() != null) {
                        for (OrthoAlgorithm algo : join.getNotMatched()) {
                            notMatched.add(algo.getName());
                        }
                    }
                    ArrayList<String> notCalled = new ArrayList<String>();
                    if (join != null && join.getNotCalled() != null) {
                        for (OrthoAlgorithm algo : join.getNotCalled()) {
                            notCalled.add(algo.getName());
                        }
                    }
                    Orthologous orth = lookup.get(join.getPrimaryKey());
                    OrthologyDoclet doc = new OrthologyDoclet(
                            orth.getPrimaryKey(),
                            orth.isBestScore(),
                            orth.isBestRevScore(),
                            orth.getConfidence(),
                            orth.getGene1().getSpecies() == null ? null : orth.getGene1().getSpecies().getPrimaryKey(),
                            orth.getGene2().getSpecies() == null ? null : orth.getGene2().getSpecies().getPrimaryKey(),
                            orth.getGene1().getSpecies() == null ? null : orth.getGene1().getSpecies().getName(),
                            orth.getGene2().getSpecies() == null ? null : orth.getGene2().getSpecies().getName(),
                            orth.getGene1().getSymbol(),
                            orth.getGene2().getSymbol(),
                            orth.getGene1().getPrimaryKey(),
                            orth.getGene2().getPrimaryKey(),
                            notCalled, matched, notMatched
                    );
                    olist.add(doc);
                }

            }
            geneDocument.setOrthology(olist);
        }

        if (entity.getDOTerms() != null) {
            List<DiseaseDocument> dlist = new ArrayList<>();
            for (DOTerm dot : entity.getDOTerms()) {
                if (translationDepth > 0) {
                    try {
                        DiseaseDocument doc = diseaseTranslator.translate(dot, translationDepth - 1); // This needs to not happen if being called from DiseaseTranslator
                        dlist.add(doc);
                    } catch (Exception e) {
                        log.error("Exception Creating Disease Document: " + e.getMessage());
                    }
                }
            }
            geneDocument.setDiseases(dlist);
        }

        if (entity.getGenomeLocations() != null) {
            List<GenomeLocationDoclet> gllist = new ArrayList<>();
            for (GenomeLocation location : entity.getGenomeLocations()) {
                GenomeLocationDoclet loc = new GenomeLocationDoclet(
                        location.getStart(),
                        location.getEnd(),
                        location.getAssembly(),
                        location.getStrand(),
                        location.getChromosome().getPrimaryKey());

                gllist.add(loc);
            }
            geneDocument.setGenomeLocations(gllist);
        }

        if (entity.getCrossReferences() != null) {
            List<CrossReferenceDoclet> crlist = entity.getCrossReferences().stream()
                    .map(crossReference -> {
                        CrossReferenceDoclet crd = new CrossReferenceDoclet();
                        crd.setCrossRefCompleteUrl(crossReference.getCrossRefCompleteUrl());
                        crd.setLocalId(crossReference.getLocalId());
                        crd.setId(String.valueOf(crossReference.getId()));
                        crd.setGlobalCrossrefId(crossReference.getGlobalCrossrefId());
                        return crd;
                    })
                    .collect(Collectors.toList());
            geneDocument.setCrossReferences(crlist);
        }
        return geneDocument;
    }

    @Override
    protected Gene documentToEntity(GeneDocument document, int translationDepth) {
        // We are not going to the database yet so will implement this when we need to
        return null;
    }

}
