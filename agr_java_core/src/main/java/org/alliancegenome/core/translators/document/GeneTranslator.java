package org.alliancegenome.core.translators.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.core.translators.doclet.CrossReferenceDocletTranslator;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.doclet.GenomeLocationDoclet;
import org.alliancegenome.es.index.site.doclet.OrthologyDoclet;
import org.alliancegenome.es.index.site.document.DiseaseDocument;
import org.alliancegenome.es.index.site.document.FeatureDocument;
import org.alliancegenome.es.index.site.document.GeneDocument;
import org.alliancegenome.es.index.site.document.PhenotypeDocument;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.OrthoAlgorithm;
import org.alliancegenome.neo4j.entity.node.OrthologyGeneJoin;
import org.alliancegenome.neo4j.entity.node.SecondaryId;
import org.alliancegenome.neo4j.entity.node.Synonym;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;
import org.apache.commons.collections4.CollectionUtils;

public class GeneTranslator extends EntityDocumentTranslator<Gene, GeneDocument> {

    //private final Logger log = LogManager.getLogger(getClass());

    private static DiseaseTranslator diseaseTranslator = new DiseaseTranslator();
    private static PhenotypeTranslator phenotypeTranslator = new PhenotypeTranslator();
    private static FeatureTranslator alleleTranslator = new FeatureTranslator();
    private static CrossReferenceDocletTranslator crossReferenceTranslator = new CrossReferenceDocletTranslator();

    @Override
    protected GeneDocument entityToDocument(Gene gene, int translationDepth) {
        //log.info(entity);
        HashMap<String, ArrayList<GOTerm>> goTerms = new HashMap<>();

        GeneDocument geneDocument = new GeneDocument();

        geneDocument.setCategory("gene");

        geneDocument.setDataProvider(gene.getDataProvider());
        geneDocument.setDescription(gene.getDescription());

        geneDocument.setGeneLiteratureUrl(gene.getGeneLiteratureUrl());
        geneDocument.setAutomatedGeneSynopsis(gene.getAutomatedGeneSynopsis());
        geneDocument.setGeneSynopsis(gene.getGeneSynopsis());
        geneDocument.setGeneSynopsisUrl(gene.getGeneSynopsisUrl());
        geneDocument.setGeneticEntityExternalUrl(gene.getGeneticEntityExternalUrl());

        geneDocument.setModCrossRefCompleteUrl(gene.getModCrossRefCompleteUrl());
        geneDocument.setModLocalId(gene.getModLocalId());
        geneDocument.setModGlobalCrossRefId(gene.getModGlobalCrossRefId());
        geneDocument.setModGlobalId(gene.getModGlobalId());
        if (gene.getName() == null)
            geneDocument.setName(gene.getSymbol());
        else
            geneDocument.setName(gene.getName());
        geneDocument.setNameKeyWithSpecies(gene.getSymbol(), gene.getSpecies().getType().getAbbreviation());
        geneDocument.setPrimaryId(gene.getPrimaryKey());
        geneDocument.setDateProduced(gene.getDateProduced());
        geneDocument.setTaxonId(gene.getTaxonId());


        if (gene.getCreatedBy() != null) {
            geneDocument.setRelease(gene.getCreatedBy().getRelease());
        }
        if (gene.getSpecies() != null) {
            geneDocument.setSpecies(gene.getSpecies().getName());
        }

        // Setup Go Terms by type
        for (GOTerm term : gene.getGOTerms()) {
            ArrayList<GOTerm> list = goTerms.get(term.getType());
            if (list == null) {
                list = new ArrayList<>();
                goTerms.put(term.getType(), list);
            } else if (!list.contains(term)) {
                list.add(term);
            }
        }
        geneDocument.setGene_biological_process(collectGoTermNames(goTerms.get("biological_process")));
        geneDocument.setGene_cellular_component(collectGoTermNames(goTerms.get("cellular_component")));
        geneDocument.setGene_molecular_function(collectGoTermNames(goTerms.get("molecular_function")));

        geneDocument.setBiologicalProcessWithParents(collectGoTermParentNames(goTerms.get("biological_process")));
        geneDocument.setCellularComponentWithParents(collectGoTermParentNames(goTerms.get("cellular_component")));
        geneDocument.setMolecularFunctionWithParents(collectGoTermParentNames(goTerms.get("molecular_function")));

        // This code is duplicated in Gene and Feature should be pulled out into its own translator
        ArrayList<String> secondaryIds = new ArrayList<>();
        if (gene.getSecondaryIds() != null) {
            for (SecondaryId secondaryId : gene.getSecondaryIds()) {
                secondaryIds.add(secondaryId.getName());
            }
        }
        geneDocument.setSecondaryIds(secondaryIds);


        if (gene.getSOTerm() != null) {
            geneDocument.setSoTermId(gene.getSOTerm().getPrimaryKey());
            geneDocument.setSoTermName(gene.getSOTerm().getName());
        }
        geneDocument.setSymbol(gene.getSymbol());

        // This code is duplicated in Gene and Feature should be pulled out into its own translator
        ArrayList<String> synonyms = new ArrayList<>();
        if (gene.getSynonyms() != null) {
            for (Synonym synonym : gene.getSynonyms()) {
                if (synonym.getPrimaryKey() != null) {
                    synonyms.add(synonym.getPrimaryKey());
                } else {
                    synonyms.add(synonym.getName());
                }
            }
        }
        geneDocument.setSynonyms(synonyms);


        //      if(entity.getOrthoGenes() != null) {
        //      if(lookup.size() + entity.getOrthologyGeneJoins().size() > 0) {
        //          System.out.println(lookup.size() + " ==? " + entity.getOrthologyGeneJoins().size());
        //      }

        if (gene.getOrthologyGeneJoins().size() > 0 && translationDepth > 0) {
            List<OrthologyDoclet> olist = new ArrayList<>();

            HashMap<String, Orthologous> lookup = new HashMap<>();
            for (Orthologous o : gene.getOrthoGenes()) {
                lookup.put(o.getPrimaryKey(), o);
            }

            for (OrthologyGeneJoin join : gene.getOrthologyGeneJoins()) {

                if (lookup.containsKey(join.getPrimaryKey())) {

                    ArrayList<String> matched = new ArrayList<>();
                    if (join.getMatched() != null) {
                        for (OrthoAlgorithm algo : join.getMatched()) {
                            matched.add(algo.getName());
                        }
                    }
                    ArrayList<String> notMatched = new ArrayList<>();
                    if (join.getNotMatched() != null) {
                        for (OrthoAlgorithm algo : join.getNotMatched()) {
                            notMatched.add(algo.getName());
                        }
                    }
                    ArrayList<String> notCalled = new ArrayList<>();
                    if (join.getNotCalled() != null) {
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

        if (gene.getDiseaseEntityJoins() != null && translationDepth > 0) {
            List<DiseaseDocument> diseaseList = diseaseTranslator.getDiseaseDocuments(gene, gene.getDiseaseEntityJoins(), translationDepth);
            geneDocument.setDiseases(diseaseList);
        }

        if (gene.getPhenotypeEntityJoins() != null && gene.getPhenotypeEntityJoins().size() > 0 && translationDepth > 0) {
            List<PhenotypeDocument> phenotypeList = phenotypeTranslator.getPhenotypeDocuments(gene, gene.getPhenotypeEntityJoins(), translationDepth);
            geneDocument.setPhenotype(phenotypeList);
        }

        if (gene.getGenomeLocations() != null) {
            List<GenomeLocationDoclet> gllist = new ArrayList<>();
            for (GenomeLocation location : gene.getGenomeLocations()) {
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

        if (gene.getCrossReferences() != null) {
            geneDocument.setCrossReferencesMap(
                    gene.getCrossReferences().stream()
                            .map(crossReference -> {
                                return crossReferenceTranslator.translate(crossReference);
                            })
                            .collect(Collectors.groupingBy(CrossReferenceDoclet::getType, Collectors.toList())));
        }

        if (gene.getFeatures() != null && translationDepth > 0) {
            List<FeatureDocument> featureList = new ArrayList<>();
            gene.getFeatures().forEach(feature ->
                    featureList.add(alleleTranslator.entityToDocument(feature, translationDepth - 1))
            );
            geneDocument.setAlleles(featureList);
        }

        return geneDocument;
    }

    protected List<String> collectGoTermNames(List<GOTerm> terms) {
        return CollectionUtils.emptyIfNull(terms)
                .stream().map(GOTerm::getName).collect(Collectors.toList());
    }

    protected List<String> collectGoTermParentNames(List<GOTerm> terms) {
        return CollectionUtils.emptyIfNull(terms).stream()
                .map(GOTerm::getParentTerms)
                .flatMap(List::stream)
                .map(GOTerm::getName)
                .collect(Collectors.toList());
    }

    @Override
    protected Gene documentToEntity(GeneDocument document, int translationDepth) {
        // We are not going to the database yet so will implement this when we need to
        return null;
    }

}
