package org.alliancegenome.core.translators.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.core.translators.doclet.CrossReferenceDocletTranslator;
import org.alliancegenome.es.index.site.doclet.CrossReferenceDoclet;
import org.alliancegenome.es.index.site.doclet.GenomeLocationDoclet;
import org.alliancegenome.es.index.site.document.AlleleDocument;
import org.alliancegenome.es.index.site.document.AnnotationDocument;
import org.alliancegenome.es.index.site.document.DiseaseDocument;
import org.alliancegenome.es.index.site.document.GeneDocument;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.SecondaryId;
import org.alliancegenome.neo4j.entity.node.Synonym;
import org.alliancegenome.neo4j.entity.relationship.GenomeLocation;
import org.alliancegenome.neo4j.entity.relationship.Orthologous;

public class GeneTranslator extends EntityDocumentTranslator<Gene, GeneDocument> {

    //private final Logger log = LogManager.getLogger(getClass());

    private static DiseaseTranslator diseaseTranslator = new DiseaseTranslator();
    private static AlleleTranslator alleleTranslator = new AlleleTranslator();
    private static CrossReferenceDocletTranslator crossReferenceTranslator = new CrossReferenceDocletTranslator();

    @Override
    protected GeneDocument entityToDocument(Gene gene, int translationDepth) {
        //log.info(entity);

        HashMap<String, Set<GOTerm>> goTerms = new HashMap<>();

        GeneDocument geneDocument = new GeneDocument();

        geneDocument.setCategory("gene");

        geneDocument.setDataProvider(gene.getDataProvider());
        geneDocument.setDescription(gene.getDescription());

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
        geneDocument.setNameKey(gene.getNameKey());
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
        for (GOTerm term : gene.getGoTerms()) {
            Set<GOTerm> terms = goTerms.get(term.getType());
            if (terms == null) {
                terms = new HashSet<>();
                goTerms.put(term.getType(), terms);
            } else {
                terms.add(term);
            }
        }

//      geneDocument.setBiologicalProcessWithParents(gene.getBiologicalProcessWithParents());
//      geneDocument.setCellularComponentWithParents(gene.getCellularComponentWithParents());
//      geneDocument.setMolecularFunctionWithParents(gene.getMolecularFunctionWithParents());
//
//      geneDocument.setBiologicalProcessAgrSlim(gene.getBiologicalProcessAgrSlim());
//      geneDocument.setCellularComponentAgrSlim(gene.getCellularComponentAgrSlim());
//      geneDocument.setMolecularFunctionAgrSlim(gene.getMolecularFunctionAgrSlim());

        // This code is duplicated in Gene and Allele should be pulled out into its own translator
        ArrayList<String> secondaryIds = new ArrayList<>();
        if (gene.getSecondaryIds() != null) {
            for (SecondaryId secondaryId : gene.getSecondaryIds()) {
                secondaryIds.add(secondaryId.getName());
            }
        }
        geneDocument.setSecondaryIds(secondaryIds);


        if (gene.getSoTerm() != null) {
            geneDocument.setSoTermId(gene.getSoTerm().getPrimaryKey());
            geneDocument.setSoTermName(gene.getSoTerm().getName());
        }
        geneDocument.setSymbol(gene.getSymbol());

        // This code is duplicated in Gene and Allele should be pulled out into its own translator
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

        geneDocument.setStrictOrthologySymbols(
                gene.getOrthoGenes().stream()
                        .filter(Orthologous::isStrictFilter)
                        .map(Orthologous::getGene2)
                        .map(Gene::getSymbol)
                        .collect(Collectors.toSet())
        );

        if (gene.getDiseaseEntityJoins() != null && translationDepth > 0) {
            List<DiseaseDocument> diseaseList = diseaseTranslator.getDiseaseDocuments(gene, gene.getDiseaseEntityJoins(), translationDepth - 1);
            // group into experiment or orthology
            // check if a doc has an orthology-related record
            List<DiseaseDocument> diseaseViaOrthology = diseaseList.stream()
                    .filter(diseaseDocument -> diseaseDocument.getAnnotations()
                            .stream().anyMatch(annotationDocument -> annotationDocument.getOrthologyGeneDocument() != null))
                    .collect(Collectors.toList());
            // create a semi-deep clone as we have to separate diseaseDocuments with the empirical or orthology annotation docs
            diseaseViaOrthology = diseaseViaOrthology.stream()
                    .map(DiseaseDocument::new).collect(Collectors.toList());
            // filter for orthology records.
            // check if a doc has other annotations than orthology
            List<DiseaseDocument> diseaseViaExperiment = diseaseList.stream()
                    .filter(diseaseDocument -> diseaseDocument.getAnnotations()
                            .stream().anyMatch(annotationDocument -> annotationDocument.getOrthologyGeneDocument() == null))
                    .collect(Collectors.toList());
            // create a semi-deep clone as we have to separate diseaseDocuments with the empirical or orthology annotation docs
            diseaseViaExperiment = diseaseViaExperiment.stream()
                    .map(DiseaseDocument::new).collect(Collectors.toList());

            // filter out the records for orthology
            diseaseViaOrthology.forEach(diseaseDocument ->
                    diseaseDocument.getAnnotations().removeIf(annotationDocument -> annotationDocument.getOrthologyGeneDocument() == null));
            geneDocument.setDiseasesViaOrthology(diseaseViaOrthology);
            // Remove orthology annotations
            diseaseViaExperiment = diseaseViaExperiment.stream()
                    .peek(diseaseDocument -> {
                        List<AnnotationDocument> docs = diseaseDocument.getAnnotations().stream()
                                .filter(annotationDocument -> annotationDocument.getPublications().stream()
                                        .anyMatch(publicationDoclet -> !publicationDoclet.getEvidenceCodes().contains("IEA")))
                                .collect(Collectors.toList());
                        diseaseDocument.setAnnotations(docs);
                    })
                    .collect(Collectors.toList());
            geneDocument.setDiseasesViaExperiment(diseaseViaExperiment);
        }

//      geneDocument.setPhenotypeStatements(gene.getPhenotypeStatements());
//
//      geneDocument.setWhereExpressed(gene.getWhereExpressed());
//      geneDocument.setAnatomicalExpression(gene.getAnatomicalExpression());
//      geneDocument.setAnatomicalExpressionWithParents(gene.getAnatomicalExpressionWithParents());
//
//      geneDocument.setCellularComponentExpressionWithParents(gene.getCellularComponentExpressionWithParents());
//      geneDocument.setCellularComponentExpressionAgrSlim(gene.getCellularComponentExpressionAgrSlim());

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

        if (gene.getAlleles() != null && translationDepth > 0) {
            List<AlleleDocument> alleleList = new ArrayList<>();
            gene.getAlleles().forEach(allele ->
            alleleList.add(alleleTranslator.entityToDocument(allele, translationDepth - 1))
            );
            // Parent class also has a variable called alleles
            //geneDocument.setAlleles(alleleList);
        }

        return geneDocument;
    }


    @Override
    protected Gene documentToEntity(GeneDocument document, int translationDepth) {
        // We are not going to the database yet so will implement this when we need to
        return null;
    }

}
