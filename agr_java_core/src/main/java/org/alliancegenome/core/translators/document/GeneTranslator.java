package org.alliancegenome.core.translators.document;

import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.core.translators.doclet.CrossReferenceDocletTranslator;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.doclet.GenomeLocationDoclet;
import org.alliancegenome.es.index.site.doclet.OrthologyDoclet;
import org.alliancegenome.es.index.site.document.DiseaseDocument;
import org.alliancegenome.es.index.site.document.FeatureDocument;
import org.alliancegenome.es.index.site.document.GeneDocument;
import org.alliancegenome.es.index.site.document.PhenotypeDocument;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class GeneTranslator extends EntityDocumentTranslator<Gene, GeneDocument> {

    //private final Logger log = LogManager.getLogger(getClass());

    private static DiseaseTranslator diseaseTranslator = new DiseaseTranslator();
    private static PhenotypeTranslator phenotypeTranslator = new PhenotypeTranslator();
    private static FeatureTranslator alleleTranslator = new FeatureTranslator();
    private static CrossReferenceDocletTranslator crossReferenceTranslator = new CrossReferenceDocletTranslator();

    @Override
    protected GeneDocument entityToDocument(Gene gene, int translationDepth) {
        //log.info(entity);
        HashMap<String, Set<GOTerm>> goTerms = new HashMap<>();

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
            Set<GOTerm> terms = goTerms.get(term.getType());
            if (terms == null) {
                terms = new HashSet<>();
                goTerms.put(term.getType(), terms);
            } else {
                terms.add(term);
            }
        }

        geneDocument.setBiologicalProcess(collectGoTermNames(goTerms.get("biological_process")));
        geneDocument.setCellularComponent(collectGoTermNames(goTerms.get("cellular_component")));
        geneDocument.setMolecularFunction(collectGoTermNames(goTerms.get("molecular_function")));

        Set<GOTerm> allParentTerms = gene.getGoParentTerms();

        geneDocument.setBiologicalProcessWithParents(collectGoTermParentNames(allParentTerms,"biological_process"));
        geneDocument.setCellularComponentWithParents(collectGoTermParentNames(allParentTerms,"cellular_component"));
        geneDocument.setMolecularFunctionWithParents(collectGoTermParentNames(allParentTerms,"molecular_function"));

        geneDocument.setBiologicalProcessAgrSlim(collectGoTermSlimNames(allParentTerms, "biological_process", "goslim_agr"));
        geneDocument.setCellularComponentAgrSlim(collectGoTermSlimNames(allParentTerms, "cellular_component", "goslim_agr"));
        geneDocument.setMolecularFunctionAgrSlim(collectGoTermSlimNames(allParentTerms, "molecular_function", "goslim_agr"));

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


        if (gene.getOrthologyGeneJoins().size() > 0 && translationDepth > 0) {
            List<OrthologyDoclet> doclets = OrthologyService.getOrthologyDoclets(gene);
            geneDocument.setOrthology(doclets);
        }

        geneDocument.setStrictOrthologySymbols(
                gene.getOrthoGenes().stream()
                .filter(Orthologous::isStrictFilter)
                .map(Orthologous::getGene2)
                .map(Gene::getSymbol)
                .distinct()
                .collect(Collectors.toList())
        );

        if (gene.getDiseaseEntityJoins() != null && translationDepth > 0) {
            List<DiseaseDocument> diseaseList = diseaseTranslator.getDiseaseDocuments(gene, gene.getDiseaseEntityJoins(), translationDepth);
            geneDocument.setDiseases(diseaseList);
        }

        geneDocument.setPhenotypeStatements(
                gene.getPhenotypes().stream()
                        .map(Phenotype::getPhenotypeStatement)
                        .collect(Collectors.toList())
        );

        if (gene.getPhenotypeEntityJoins() != null && gene.getPhenotypeEntityJoins().size() > 0 && translationDepth > 0) {
            List<PhenotypeDocument> phenotypeList = phenotypeTranslator.getPhenotypeDocuments(gene, gene.getPhenotypeEntityJoins(), translationDepth);
            geneDocument.setPhenotypes(phenotypeList);
        }

        geneDocument.setExpressionBioEntities(
                gene.getExpressionBioEntities().stream()
                .map(ExpressionBioEntity::getWhereExpressedStatement)
                .distinct()
                .collect(Collectors.toList())
        );

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

    protected List<String> collectGoTermNames(Set<GOTerm> terms) {
        return CollectionUtils.emptyIfNull(terms)
                .stream().map(GOTerm::getName).collect(Collectors.toList());
    }

    protected List<String> collectGoTermParentNames(Set<GOTerm> terms, String type) {
        return CollectionUtils.emptyIfNull(terms).stream()
                .filter(term -> term.getType().equals(type))
                .map(GOTerm::getName)
                .collect(Collectors.toList());
    }

    protected List<String> collectGoTermSlimNames(Set<GOTerm> terms, String type, String subset) {
        return CollectionUtils.emptyIfNull(terms).stream()
                .filter(term -> term.getSubset().contains(subset))
                .filter(term -> term.getType().equals(type))
                .map(GOTerm::getName)
                .collect(Collectors.toList());
    }

    @Override
    protected Gene documentToEntity(GeneDocument document, int translationDepth) {
        // We are not going to the database yet so will implement this when we need to
        return null;
    }

}
