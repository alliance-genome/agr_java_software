package org.alliancegenome.indexer.translators;

import org.alliancegenome.indexer.document.*;
import org.alliancegenome.indexer.entity.SpeciesType;
import org.alliancegenome.indexer.entity.node.*;
import org.alliancegenome.indexer.service.SpeciesService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class DiseaseTranslator extends EntityDocumentTranslator<DOTerm, DiseaseDocument> {

    private final GeneTranslator geneTranslator = new GeneTranslator();

    private final Logger log = LogManager.getLogger(getClass());

    @Override
    protected DiseaseDocument entityToDocument(DOTerm entity, int translationDepth) {
        return entityToDocument(entity, null, translationDepth);
    }


    protected DiseaseDocument entityToDocument(DOTerm entity, Gene gene, int translationDepth) {
        DiseaseDocument doc = getTermDiseaseDocument(entity);

        if (entity.getDiseaseGeneJoins() == null || entity.getDiseaseFeatureJoins() == null)
            return doc;

        // this map will leave out genes that have at least one allele / feature associated
        //
        Map<Gene, Map<String, List<DiseaseGeneJoin>>> sortedGeneAssociationMap = getGeneAnnotationMap(entity, gene);

        // generate AnnotationDocument records
        List<AnnotationDocument> annotationDocuments = sortedGeneAssociationMap.entrySet().stream()
                .map(geneMapEntry ->
                        geneMapEntry.getValue().entrySet().stream().map(associationEntry -> {
                            AnnotationDocument document = new AnnotationDocument();
                            if (translationDepth > 0) {
                                document.setGeneDocument(geneTranslator.translate(geneMapEntry.getKey(), translationDepth - 1)); // This needs to not happen if being call from GeneTranslator
                            }
                            document.setAssociationType(associationEntry.getKey());
                            document.setSource(getSourceUrls(entity, geneMapEntry.getKey().getSpecies()));
                            document.setPublications(getPublicationDoclets(associationEntry));
                            return document;
                        }).collect(Collectors.toList()))
                // turn List<AnnotationDocument> into stream<AnnotationDocument> so they can be collected into
                // the outer List<AnnotationDocument>
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // add features
        Map<Feature, Map<String, List<DiseaseFeatureJoin>>> sortedFeatureAssociationMap = getFeatureAnnotationMap(entity, gene);
        List<AnnotationDocument> featureAnnotationDocuments = sortedFeatureAssociationMap.entrySet().stream()
                .map(featureMapEntry ->
                        featureMapEntry.getValue().entrySet().stream().map(associationEntry -> {
                            AnnotationDocument document = new AnnotationDocument();
                            Feature feature = featureMapEntry.getKey();
                            if (translationDepth > 0) {
                                document.setGeneDocument(geneTranslator.translate(feature.getGene(), translationDepth - 1)); // This needs to not happen if being call from GeneTranslator
                            }
                            FeatureDocument fDocument = new FeatureDocument();
                            fDocument.setPrimaryKey(feature.getPrimaryKey());
                            fDocument.setSymbol(feature.getSymbol());
                            document.setFeatureDocument(fDocument);
                            document.setAssociationType(associationEntry.getKey());
                            document.setSource(getSourceUrls(entity, feature.getSpecies()));
                            //ToDO: Make DiseaseXJoin Interface for next call
                            // document.setPublications(getPublicationDoclets(associationEntry));
                            return document;
                        }).collect(Collectors.toList()))
                // turn List<AnnotationDocument> into stream<AnnotationDocument> so they can be collected into
                // the outer List<AnnotationDocument>
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        if (annotationDocuments == null)
            annotationDocuments = new ArrayList<>();
        annotationDocuments.addAll(featureAnnotationDocuments);
        doc.setAnnotations(annotationDocuments);

        return doc;
    }

    private Map<Gene, Map<String, List<DiseaseGeneJoin>>> getGeneAnnotationMap(DOTerm entity, Gene gene) {
        // group by gene then by association type
        Map<Gene, Map<String, List<DiseaseGeneJoin>>> geneAssociationMap = entity.getDiseaseGeneJoins().stream()
                .filter(diseaseGeneJoin -> gene == null || diseaseGeneJoin.getGene().equals(gene))
                .filter(diseaseGeneJoin -> diseaseGeneJoin.getGene().getFeatures().isEmpty())
                .collect(
                        groupingBy(DiseaseGeneJoin::getGene,
                                groupingBy(DiseaseGeneJoin::getJoinType))
                );

        // sort by gene symbol
        return geneAssociationMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<Feature, Map<String, List<DiseaseFeatureJoin>>> getFeatureAnnotationMap(DOTerm entity, Gene gene) {
        // group by gene then by association type
        Map<Feature, Map<String, List<DiseaseFeatureJoin>>> featureAssociationMap = entity.getDiseaseFeatureJoins().stream()
                .filter(diseaseFeatureJoin -> gene == null || diseaseFeatureJoin.getFeature().getGene().equals(gene))
                .collect(
                        groupingBy(DiseaseFeatureJoin::getFeature,
                                groupingBy(DiseaseFeatureJoin::getJoinType))
                );

        return featureAssociationMap;
    }

    private PublicationDoclet getPublicationDoclet(DiseaseGeneJoin association, Publication publication) {
        PublicationDoclet pubDoc = new PublicationDoclet();
        pubDoc.setPrimaryKey(publication.getPrimaryKey());

        pubDoc.setPubMedId(publication.getPubMedId());
        pubDoc.setPubMedUrl(publication.getPubMedUrl());
        pubDoc.setPubModId(publication.getPubModId());
        pubDoc.setPubModUrl(publication.getPubModUrl());

        if (association.getEvidenceCodes() == null) {
            log.error("Could not find any evidence codes for " + association.getGene().getPrimaryKey() + " and publication " + publication.getPrimaryKey());
            return null;
        }

        Set<String> evidencesDocument = association.getEvidenceCodes().stream()
                .map(EvidenceCode::getPrimaryKey)
                .collect(Collectors.toSet());
        pubDoc.setEvidenceCodes(evidencesDocument);
        return pubDoc;
    }

    private DiseaseDocument getTermDiseaseDocument(DOTerm doTerm) {
        return getTermDiseaseDocument(doTerm, false);
    }


    private DiseaseDocument getTermDiseaseDocument(DOTerm doTerm, boolean shallow) {
        DiseaseDocument document = new DiseaseDocument();
        if (doTerm.getDoId() != null)
            document.setDoId(doTerm.getDoId());
        document.setPrimaryKey(doTerm.getPrimaryKey());
        document.setPrimaryId(doTerm.getPrimaryKey());
        document.setName(doTerm.getName());
        document.setName_key(doTerm.getName());
        document.setDefinition(doTerm.getDefinition());
        document.setDefinitionLinks(doTerm.getDefLinks());
        document.setDateProduced(doTerm.getDateProduced());
        //document.setParentDiseaseNames(getParentNameList(doTerm));
        if (doTerm.getSynonyms() != null) {
            List<String> synonymList = doTerm.getSynonyms().stream()
                    .map(Synonym::getPrimaryKey)
                    .collect(Collectors.toList());
            document.setSynonyms(synonymList);
        }
        // add CrossReferences
        if (doTerm.getCrossReferences() != null) {
            List<CrossReferenceDoclet> externalIds = doTerm.getCrossReferences().stream()
                    .map(crossReference -> {
                        CrossReferenceDoclet doclet = new CrossReferenceDoclet();
                        doclet.setLocalId(crossReference.getLocalId());
                        doclet.setCrossRefCompleteUrl(crossReference.getCrossRefCompleteUrl());
                        doclet.setPrefix(crossReference.getPrefix());
                        doclet.setName(crossReference.getPrimaryKey());
                        return doclet;
                    })
                    .collect(Collectors.toList());
            document.setCrossReferences(externalIds);
        }
        if (shallow)
            return document;

        // set parents
        if (doTerm.getParents() != null) {
            List<DiseaseDocument> parentDocs = doTerm.getParents().stream()
                    .map(term -> getTermDiseaseDocument(term, true))
                    .collect(Collectors.toList());
            document.setParents(parentDocs);
        }

        // set children
        if (doTerm.getChildren() != null) {
            List<DiseaseDocument> childrenDocs = doTerm.getChildren().stream()
                    .map(term -> getTermDiseaseDocument(term, true))
                    .collect(Collectors.toList());
            document.setChildren(childrenDocs);
        }

        // set highLevelSlim values
        if (CollectionUtils.isNotEmpty(doTerm.getHighLevelTermList())) {
            doTerm.getHighLevelTermList().forEach(slimTerm ->
                    document.getHighLevelSlimTermNames().add(slimTerm.getName()));
        }

        // set all parent Names
        if (CollectionUtils.isNotEmpty(doTerm.getHighLevelTermList())) {
            doTerm.getHighLevelTermList().forEach(slimTerm ->
                    document.getHighLevelSlimTermNames().add(slimTerm.getName()));
        }

        // set all sources except Human
        document.setSourceList(getSourceUrls(doTerm).stream()
                .filter(sourceDoclet -> !sourceDoclet.getSpecies().getTaxonID().equals(SpeciesType.HUMAN.getTaxonID()))
                .collect(Collectors.toList()));

        return document;
    }

    private SourceDoclet getSourceUrls(DOTerm doTerm, Species species) {
        List<SourceDoclet> sources;
        sources = getSourceUrls(doTerm).stream().
                filter(sourceUrl ->
                        sourceUrl.getSpecies().getTaxonID().equals(species.getType().getTaxonID())
                )
                .collect(Collectors.toList());
        if (sources.isEmpty())
            return null;
        return sources.get(0);
    }

    private List<SourceDoclet> getSourceUrls(DOTerm doTerm) {

        List<SourceDoclet> sourceDoclets = Arrays.stream(SpeciesType.values())
                .map(speciesType -> {
                    SourceDoclet doclet = new SourceDoclet();
                    doclet.setSpecies(SpeciesService.getSpeciesDoclet(speciesType));
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
    protected DOTerm documentToEntity(DiseaseDocument document, int translationDepth) {
        return null;
    }

    public Iterable<DiseaseAnnotationDocument> translateAnnotationEntities(List<DOTerm> geneDiseaseList, int translationDepth) {
        Set<DiseaseAnnotationDocument> diseaseAnnotationDocuments = new HashSet<>();
        geneDiseaseList.forEach(doTerm -> {
            if (doTerm.getDiseaseGeneJoins() == null) {
                DiseaseAnnotationDocument doc = new DiseaseAnnotationDocument();
                doc.setPrimaryKey(doTerm.getPrimaryKey());
                doc.setDiseaseID(doTerm.getPrimaryKey());
                doc.setDiseaseName(doTerm.getName());
                diseaseAnnotationDocuments.add(doc);
            } else {
                Map<Gene, Map<String, List<DiseaseGeneJoin>>> sortedGeneAssociationMap = getGeneAnnotationMap(doTerm, null);
                Set<DiseaseAnnotationDocument> docSet = sortedGeneAssociationMap.entrySet().stream()
                        .map(geneMapEntry ->
                                geneMapEntry.getValue().entrySet().stream().map(associationEntry -> {
                                    Gene gene = geneMapEntry.getKey();
                                    DiseaseAnnotationDocument document = new DiseaseAnnotationDocument();
                                    if (translationDepth > 0) {
                                        document.setGeneDocument(geneTranslator.translate(gene, translationDepth - 1)); // This needs to not happen if being call from GeneTranslator
                                    }
                                    document.setPrimaryKey(doTerm.getPrimaryKey() + ":" + gene.getPrimaryKey());
                                    document.setDiseaseName(doTerm.getName());
                                    document.setDiseaseID(doTerm.getPrimaryKey());
                                    document.setParentDiseaseIDs(getParentIdList(doTerm));
                                    document.setAssociationType(associationEntry.getKey());
                                    document.setSpecies(getSpeciesDoclet(gene));
                                    document.setSource(getSourceUrls(doTerm, gene.getSpecies()));
                                    document.setPublications(getPublicationDoclets(associationEntry));
                                    return document;
                                }).collect(Collectors.toList()))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
                diseaseAnnotationDocuments.addAll(docSet);
            }
        });
        return diseaseAnnotationDocuments;
    }

    private List<PublicationDoclet> getPublicationDoclets(Map.Entry<String, List<DiseaseGeneJoin>> associationEntry) {
        Set<PublicationDoclet> publicationDocuments = associationEntry.getValue().stream()
                // filter out records that do not have valid pub / evidence code entries
                .filter(diseaseGeneJoin ->
                        getPublicationDoclet(diseaseGeneJoin, diseaseGeneJoin.getPublication()) != null
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
                existingDoclet.getEvidenceCodes().addAll(doclet.getEvidenceCodes());
            }
        }
        return pubDocletList;
    }

    /**
     * Get all the parent termIDs compiled.
     */
    private Set<String> getParentIdList(DOTerm doTerm) {
        Set<String> idList = new LinkedHashSet<>();
        idList.add(doTerm.getPrimaryKey());
        doTerm.getParents().forEach(term -> {
            idList.add(term.getPrimaryKey());
            if (term.getParents() != null)
                idList.addAll(getParentIdList(term));
        });
        return idList;
    }

    /**
     * Get all the parent term names compiled.
     */
    private Set<String> getParentNameList(DOTerm doTerm) {
        Set<String> nameList = new LinkedHashSet<>();
        nameList.add(doTerm.getName());
        doTerm.getParents().forEach(term -> {
            nameList.add(term.getName());
            if (term.getParents() != null)
                nameList.addAll(getParentNameList(term));
        });
        return nameList;
    }

    private SpeciesDoclet getSpeciesDoclet(DiseaseGeneJoin diseaseGeneJoin) {
        return getSpeciesDoclet(diseaseGeneJoin.getGene());
    }

    private SpeciesDoclet getSpeciesDoclet(Gene gene) {
        Species species = gene.getSpecies();
        SpeciesType type = species.getType();
        SpeciesDoclet doclet = new SpeciesDoclet();
        doclet.setName(species.getName());
        doclet.setTaxonID(species.getPrimaryKey());
        doclet.setOrderID(type.ordinal());
        return doclet;
    }
}
