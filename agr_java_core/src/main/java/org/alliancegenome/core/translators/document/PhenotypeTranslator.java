package org.alliancegenome.core.translators.document;

import org.alliancegenome.core.translators.EntityDocumentTranslator;
import org.alliancegenome.core.translators.doclet.CrossReferenceDocletTranslator;
import org.alliancegenome.es.index.site.doclet.PublicationDoclet;
import org.alliancegenome.es.index.site.doclet.SourceDoclet;
import org.alliancegenome.es.index.site.doclet.SpeciesDoclet;
import org.alliancegenome.es.index.site.document.DiseaseAnnotationDocument;
import org.alliancegenome.es.index.site.document.PhenotypeAnnotationDocument;
import org.alliancegenome.es.index.site.document.PhenotypeDocument;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class PhenotypeTranslator extends EntityDocumentTranslator<Phenotype, PhenotypeDocument> {

    private static GeneTranslator geneTranslator = new GeneTranslator();
    private static FeatureTranslator featureTranslator = new FeatureTranslator();

    private final Logger log = LogManager.getLogger(getClass());

    @Override
    protected PhenotypeDocument entityToDocument(Phenotype phenotype, int translationDepth) {
        PhenotypeDocument doc = getPhenotypeDocument(phenotype);

        if (phenotype.getPhenotypeEntityJoins() == null)
            return doc;

        Map<Gene, List<PhenotypeEntityJoin>> sortedGeneMap = getGeneAnnotationMap(phenotype, null);
        List<PhenotypeAnnotationDocument> annotationDocuments = generateAnnotationDocument(phenotype, sortedGeneMap);
        doc.setAnnotations(annotationDocuments);
        return doc;
    }

    // list of phenotypeEntityJoins are of type phenotype and gene
    private PhenotypeDocument entityToDocument(Phenotype phenotype, Gene gene, List<PhenotypeEntityJoin> dejList) {
        PhenotypeDocument doc = getPhenotypeDocument(phenotype);

        if (dejList == null)
            return doc;

        Map<Gene, List<PhenotypeEntityJoin>> map = new HashMap<>();
        map.put(gene, dejList);
        // create AnnotationDocument objects per
        // disease, gene, Feature, association type
        List<PhenotypeAnnotationDocument> annotationDocuments = generateAnnotationDocument(phenotype, map);
        doc.setAnnotations(annotationDocuments);
        return doc;
    }

    public List<PhenotypeDocument> getPhenotypeDocuments(Gene gene, List<PhenotypeEntityJoin> phenotypeJoins, int translationDepth) {
        // group by phenotype

        phenotypeJoins.forEach(join -> {
            if (join.getPhenotype() == null)
                System.out.println(gene.getPrimaryKey());
        });

        Map<Phenotype, List<PhenotypeEntityJoin>> phenotypeMap = phenotypeJoins.stream()
                .filter(join -> join.getPhenotype() != null)
                .collect(Collectors.groupingBy(PhenotypeEntityJoin::getPhenotype));
        List<PhenotypeDocument> phenotypeList = new ArrayList<>();
        // for each phenotype create annotation doc
        // phenotypeEntityJoin list turns into AnnotationDocument objects
        phenotypeMap.forEach((phenotype, phenotypeEntityJoins) -> {
            if (translationDepth > 0) {
                try {
                    PhenotypeDocument doc = entityToDocument(phenotype, gene, phenotypeEntityJoins); // This needs to not happen if being called from DiseaseTranslator
                    if (!phenotypeList.contains(doc))
                        phenotypeList.add(doc);
                } catch (Exception e) {
                    log.error("Exception Creating Phenotype Document: " + e.getMessage());
                }
            }

        });
        return phenotypeList;
    }

    private List<PhenotypeAnnotationDocument> generateAnnotationDocument(Phenotype phenotype, Map<Gene, List<PhenotypeEntityJoin>> sortedGeneMap) {
        // generate PhenotypeAnnotationDocument records
        return sortedGeneMap.entrySet().stream()
                .map(geneMapEntry -> {
                    List<PhenotypeEntityJoin> featureJoins = geneMapEntry.getValue().stream()
                            .filter(join -> join.getFeature() != null)
                            .collect(toList());
                    List<PhenotypeEntityJoin> featurelessJoins = geneMapEntry.getValue().stream()
                            .filter(join -> join.getFeature() == null)
                            .collect(toList());

                    Map<Feature, List<PhenotypeEntityJoin>> featureMap = featureJoins.stream()
                            .filter(entry -> phenotype != null)
                            .collect(Collectors.groupingBy(PhenotypeEntityJoin::getFeature
                            ));
                    if (!featurelessJoins.isEmpty())
                        featureMap.put(null, featurelessJoins);
                    return featureMap.entrySet().stream()
                            .map(featureMapEntry -> {
                                PhenotypeAnnotationDocument document = new PhenotypeAnnotationDocument();
                                document.setGeneDocument(geneTranslator.translate(geneMapEntry.getKey(), 0));
                                Feature feature = featureMapEntry.getKey();
                                if (feature != null) {
                                    document.setFeatureDocument(featureTranslator.translate(feature, 0));
                                }
                                document.setPublications(getPublicationDoclets(featureMapEntry.getValue()));
                                return document;
                            }).collect(Collectors.toList());
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private Map<Gene, List<PhenotypeEntityJoin>> getGeneAnnotationMap(Phenotype phenotype, Gene gene) {
        // group by gene then by association type
        Map<Gene, List<PhenotypeEntityJoin>> geneMap = phenotype.getPhenotypeEntityJoins().stream()
                .filter(phenotypeEntityJoin -> gene == null || phenotypeEntityJoin.getGene().equals(gene))
                .collect(groupingBy(PhenotypeEntityJoin::getGene));

        // sort by gene symbol
        return geneMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<Gene, Map<Optional<Feature>, List<DiseaseEntityJoin>>> getGeneFeatureAnnotationMap(DOTerm entity, Gene gene) {
        // group by gene then by feature
        Map<Gene, Map<Optional<Feature>, List<DiseaseEntityJoin>>> geneAssociationMap = entity.getDiseaseEntityJoins().stream()
                .filter(diseaseEntityJoin -> gene == null || diseaseEntityJoin.getGene().equals(gene))
                .collect(
                        groupingBy(DiseaseEntityJoin::getGene,
                                groupingBy(join -> Optional.ofNullable(join.getFeature())))
                );

        // sort by gene symbol
        return geneAssociationMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private PublicationDoclet getPublicationDoclet(PhenotypeEntityJoin association, Publication publication) {
        PublicationDoclet pubDoc = new PublicationDoclet();
        pubDoc.setPrimaryKey(publication.getPrimaryKey());

        pubDoc.setPubMedId(publication.getPubMedId());
        pubDoc.setPubMedUrl(publication.getPubMedUrl());
        pubDoc.setPubModId(publication.getPubModId());
        pubDoc.setPubModUrl(publication.getPubModUrl());

/*
        Set<String> evidencesDocument = association.getEvidenceCodes().stream()
                .map(EvidenceCode::getPrimaryKey)
                .collect(Collectors.toSet());
        pubDoc.setEvidenceCodes(evidencesDocument);
*/
        return pubDoc;
    }

    private PhenotypeDocument getPhenotypeDocument(Phenotype phenotype) {
        return getPhenotypeDocument(phenotype, false);
    }


    private PhenotypeDocument getPhenotypeDocument(Phenotype phenotype, boolean shallow) {
        PhenotypeDocument document = new PhenotypeDocument();
        document.setPrimaryKey(phenotype.getPrimaryKey());
        document.setPrimaryId(phenotype.getPrimaryKey());
        document.setPhenotypeStatement(phenotype.getPhenotypeStatement());
        if (shallow)
            return document;

        return document;
    }

    private List<SourceDoclet> getSourceUrls(DOTerm doTerm) {

        List<SourceDoclet> sourceDoclets = Arrays.stream(SpeciesType.values())
                .map(speciesType -> {
                    SourceDoclet doclet = new SourceDoclet();
                    doclet.setSpecies(speciesType.getDoclet());
                    doclet.setName(speciesType.getDisplayName());
                    if (speciesType.equals(SpeciesType.HUMAN)) {
                        doclet.setName(SpeciesType.RAT.getDisplayName());
                        doclet.setDiseaseUrl(doTerm.getHumanOnlyRgdLink());
                    }
                    if (speciesType == SpeciesType.FLY && doTerm.getFlybaseLink() != null) {
                        doclet.setUrl(doTerm.getFlybaseLink());
                        doclet.setDiseaseUrl(doTerm.getFlybaseLink());
                    }
                    if (speciesType == SpeciesType.RAT && doTerm.getRgdLink() != null) {
                        doclet.setUrl(doTerm.getRgdLink());
                        doclet.setDiseaseUrl(doTerm.getRatOnlyRgdLink());
                    }
                    if (speciesType == SpeciesType.MOUSE && doTerm.getMgiLink() != null) {
                        doclet.setUrl(doTerm.getMgiLink());
                        doclet.setDiseaseUrl(doTerm.getMgiLink());
                    }
                    if (speciesType == SpeciesType.ZEBRAFISH && doTerm.getZfinLink() != null) {
                        doclet.setUrl(doTerm.getZfinLink());
                        doclet.setDiseaseUrl(doTerm.getZfinLink());
                    }
                    if (speciesType == SpeciesType.HUMAN && doTerm.getHumanLink() != null) {
                        doclet.setUrl(doTerm.getHumanLink());
                    }
                    if (speciesType == SpeciesType.WORM && doTerm.getWormbaseLink() != null) {
                        doclet.setUrl(doTerm.getWormbaseLink());
                        doclet.setDiseaseUrl(doTerm.getWormbaseLink());
                    }
                    return doclet;
                })
                .collect(toList());

        return sourceDoclets;
    }

    @Override
    protected Phenotype documentToEntity(PhenotypeDocument document, int translationDepth) {
        return null;
    }

    private List<PublicationDoclet> getPublicationDoclets(List<PhenotypeEntityJoin> phenotypeEntityJoins) {
        Set<PublicationDoclet> publicationDocuments = phenotypeEntityJoins.stream()
                // filter out records that do not have valid pub / evidence code entries
                .filter(phenotypeEntityJoin ->
                        getPublicationDoclet(phenotypeEntityJoin, phenotypeEntityJoin.getPublication()) != null
                )
                .map(diseaseGeneJoin -> {
                    Publication publication = diseaseGeneJoin.getPublication();
                    return getPublicationDoclet(diseaseGeneJoin, publication);
                })
                .collect(Collectors.toSet());
        List<PublicationDoclet> pubDocletListRaw = new ArrayList<>(publicationDocuments);
        pubDocletListRaw.sort(PublicationDoclet::compareTo);

        // get evidence codes for same pub onto s
        List<PublicationDoclet> pubDocletList = new ArrayList<>();
        for (PublicationDoclet doclet : pubDocletListRaw) {
            PublicationDoclet existingDoclet = null;
            for (PublicationDoclet finalDoclet : pubDocletList) {
                if (doclet.compareTo(finalDoclet) == 0) {
                    existingDoclet = finalDoclet;
                }
            }
            if (existingDoclet == null) {
                pubDocletList.add(doclet);
            } else {
//                existingDoclet.getEvidenceCodes().addAll(doclet.getEvidenceCodes());
            }
        }
        return pubDocletList;
    }

    private SpeciesDoclet getSpeciesDoclet(Gene gene) {
        return gene.getSpecies().getType().getDoclet();
    }
}
