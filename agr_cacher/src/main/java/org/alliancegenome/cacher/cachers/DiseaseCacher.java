package org.alliancegenome.cacher.cachers;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.api.service.DiseaseRibbonService;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.DiseaseAllianceCacheManager;
import org.alliancegenome.cache.manager.ModelAllianceCacheManager;
import org.alliancegenome.cache.repository.DiseaseCacheRepository;
import org.alliancegenome.core.service.DiseaseAnnotationSorting;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponseDiseaseAnnotation;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.DiseaseEntityJoin;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.PublicationJoin;
import org.alliancegenome.neo4j.entity.node.SequenceTargetingReagent;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class DiseaseCacher extends Cacher {

    private static DiseaseRepository diseaseRepository = new DiseaseRepository();
    private static DiseaseCacheRepository diseaseCacheRepository = new DiseaseCacheRepository();

    protected void cache() {

        startProcess("diseaseRepository.getAllDiseaseEntityJoins");

        // model type of diseases
        populateModelsWithDiseases();

        Set<DiseaseEntityJoin> joinList = diseaseRepository.getAllDiseaseEntityJoins();
        if (joinList == null)
            return;

        if (useCache) {
            joinList = joinList.stream()
                    //.filter(diseaseEntityJoin -> diseaseEntityJoin.getGene() != null)
                    //.filter(diseaseEntityJoin -> diseaseEntityJoin.getAllele() != null)
                    //.filter(diseaseEntityJoin -> diseaseEntityJoin.getGene().getPrimaryKey().equals("ZFIN:ZDB-GENE-040426-1716"))
                    //.filter(diseaseEntityJoin -> diseaseEntityJoin.getAllele().getPrimaryKey().equals("FB:FBgn0030343"))
                    //.filter(diseaseEntityJoin -> diseaseEntityJoin.getGene().getPrimaryKey().equals("FB:FBgn0030343"))
                    .collect(toSet());
        }
        finishProcess();

        DiseaseRibbonService diseaseRibbonService = new DiseaseRibbonService();

        startProcess("diseaseRepository.getAllDiseaseEntityJoins");

        List<DiseaseAnnotation> allDiseaseAnnotations = getDiseaseAnnotationsFromDEJs(joinList, diseaseRibbonService);

        finishProcess();

        log.info("Number of DiseaseAnnotation object before merge: " + String.format("%,d", allDiseaseAnnotations.size()));
        // merge disease Annotations with the same
        // disease / gene / association type combination
        mergeDiseaseAnnotationsByAGM(allDiseaseAnnotations);
        log.info("Number of DiseaseAnnotation object after merge: " + String.format("%,d", allDiseaseAnnotations.size()));


        // default sorting
        DiseaseAnnotationSorting sorting = new DiseaseAnnotationSorting();
        allDiseaseAnnotations.sort(sorting.getComparator(SortingField.DEFAULT, Boolean.TRUE));

        Map<String, Set<String>> closureMapping = diseaseRepository.getClosureMapping();
        log.info("Number of Disease IDs: " + closureMapping.size());
        final Set<String> allIDs = closureMapping.keySet();

        // loop over all disease IDs (termID)
        // and store the annotations in a map for quick retrieval

        Map<String, List<DiseaseAnnotation>> diseaseAnnotationTermMap = allDiseaseAnnotations.stream()
                .collect(groupingBy(annotation -> annotation.getDisease().getPrimaryKey()));

        Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap = new HashMap<>();
        allIDs.forEach(termID -> {
            Set<String> allDiseaseIDs = closureMapping.get(termID);
            List<DiseaseAnnotation> allAnnotations = new ArrayList<>();
            allDiseaseIDs.stream()
                    .filter(id -> diseaseAnnotationTermMap.get(id) != null)
                    .forEach(id -> allAnnotations.addAll(diseaseAnnotationTermMap.get(id)));
            diseaseAnnotationMap.put(termID, allAnnotations);
        });

        setCacheStatus(joinList.size(), CacheAlliance.DISEASE_ANNOTATION.getCacheName());

        // Create map with genes as keys and their associated disease annotations as values
        // Map<gene ID, List<DiseaseAnnotation>> including annotations to child terms
        Map<String, List<DiseaseAnnotation>> diseaseAnnotationExperimentGeneMap = allDiseaseAnnotations.stream()
                .filter(annotation -> annotation.getSortOrder() < 10)
                .filter(annotation -> annotation.getGene() != null)
                .collect(groupingBy(o -> o.getGene().getPrimaryKey(), Collectors.toList()));
        diseaseAnnotationMap.putAll(diseaseAnnotationExperimentGeneMap);

        log.info("Number of Disease IDs in disease Map after adding gene grouping: " + diseaseAnnotationMap.size());
        DiseaseAllianceCacheManager manager = new DiseaseAllianceCacheManager();
        diseaseAnnotationMap.forEach((key, value) -> {
            JsonResultResponseDiseaseAnnotation result = new JsonResultResponseDiseaseAnnotation();
            result.setResults(value);
            try {
                manager.putCache(key, result, View.DiseaseCacher.class, CacheAlliance.DISEASE_ANNOTATION);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        // take care of allele
        if (populateAllelesCache(diseaseRibbonService, closureMapping, allIDs, manager)) return;


        diseaseRepository.clearCache();

    }

    private void populateModelsWithDiseases() {
        // model type of diseases
        List<DiseaseEntityJoin> pureAgmDiseases = diseaseRepository.getAllDiseaseAnnotationsPureAGM();
        log.info("Retrieved " + String.format("%,d", pureAgmDiseases.size()) + " DiseaseEntityJoin records for pure AGMs");
        // set the gene object on the join


        // phenotypeEntityJoin PK, List<Gene>
        Map<String, List<Gene>> modelGenesMap = new HashMap<>();

        pureAgmDiseases.stream()
                .filter(join -> org.apache.commons.collections.CollectionUtils.isNotEmpty(join.getModel().getAlleles()))
                .forEach(join -> {
                    Set<Gene> geneList = join.getModel().getAlleles().stream()
                            .map(Allele::getGene)
                            .collect(toSet());
                    final String primaryKey = join.getPrimaryKey();
                    List<Gene> genes = modelGenesMap.get(primaryKey);
                    if (genes == null) {
                        genes = new ArrayList<>();
                    }
                    genes.addAll(geneList);
                    genes = genes.stream().distinct().collect(toList());
                    modelGenesMap.put(primaryKey, genes);
                });
        pureAgmDiseases.stream()
                .filter(join -> org.apache.commons.collections.CollectionUtils.isNotEmpty(join.getModel().getAlleles()))
                .forEach(join -> {
                    Set<Gene> geneList = join.getModel().getSequenceTargetingReagents().stream()
                            .map(SequenceTargetingReagent::getGene)
                            .collect(toSet());
                    final String primaryKey = join.getPrimaryKey();
                    List<Gene> genes = modelGenesMap.get(primaryKey);
                    if (genes == null) {
                        genes = new ArrayList<>();
                    }
                    genes.addAll(geneList);
                    genes = genes.stream().distinct().collect(toList());
                    modelGenesMap.put(primaryKey, genes);
                });

        List<DiseaseAnnotation> allDiseaseAnnotationsPure = pureAgmDiseases.stream()
                .map(join -> {
                    DiseaseAnnotation document = new DiseaseAnnotation();
                    final AffectedGenomicModel model = join.getModel();
                    document.setModel(model);
                    document.setPrimaryKey(join.getPrimaryKey());
                    document.setDisease(join.getDisease());
                    document.setPublications(join.getPublications());

                    PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
                    entity.setId(model.getPrimaryKey());
                    entity.setEntityJoinPk(join.getPrimaryKey());
                    entity.setName(model.getName());
                    entity.setDisplayName(model.getNameText());
                    entity.setUrl(model.getModCrossRefCompleteUrl());
                    entity.setType(GeneticEntity.CrossReferenceType.getCrossReferenceType(model.getSubtype()));
                    entity.addPublicationEvidenceCode(join.getPublicationJoins());
                    entity.addDisease(join.getDisease());
                    entity.setDataProvider(model.getDataProvider());
                    document.addPrimaryAnnotatedEntity(entity);
                    return document;
                })
                .collect(Collectors.toList());

        Map<String, DiseaseAnnotation> paMap = allDiseaseAnnotationsPure.stream()
                .collect(toMap(DiseaseAnnotation::getPrimaryKey, entity -> entity));
        // merge annotations with the same model
        // geneID, Map<modelID, List<PhenotypeAnnotation>>>
/*
        Map<String, Map<String, List<PhenotypeAnnotation>>> annotationPureMergeMap = allDiseaseAnnotationsPure.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGene().getPrimaryKey(), groupingBy(annotation -> annotation.getModel().getPrimaryKey())));
*/
        Map<String, Map<String, List<DiseaseAnnotation>>> annotationPureMergeMap = new HashMap<>();

        modelGenesMap.forEach((diseaseEntityJoinID, genes) -> {
            DiseaseAnnotation diseaseAnnot = paMap.get(diseaseEntityJoinID);

            genes.forEach(gene -> {
                Map<String, List<DiseaseAnnotation>> annotations = annotationPureMergeMap.get(gene.getPrimaryKey());
                if (annotations == null) {
                    annotations = new HashMap<>();
                    annotationPureMergeMap.put(gene.getPrimaryKey(), annotations);
                }

                List<DiseaseAnnotation> dease = annotations.get(diseaseAnnot.getModel().getPrimaryKey());
                if (dease == null) {
                    dease = new ArrayList<>();
                    annotations.put(diseaseAnnot.getModel().getPrimaryKey(), dease);
                }
                dease.add(diseaseAnnot);
            });
        });


        Map<String, List<PrimaryAnnotatedEntity>> diseaseAnnotationPureMap = new HashMap<>();
        annotationPureMergeMap.forEach((geneID, modelIdMap) -> modelIdMap.forEach((modelID, diseaseAnnotations) -> {
            List<PrimaryAnnotatedEntity> mergedAnnotations = diseaseAnnotationPureMap.get(geneID);
            if (mergedAnnotations == null)
                mergedAnnotations = new ArrayList<>();
            PrimaryAnnotatedEntity entity = diseaseAnnotations.get(0).getPrimaryAnnotatedEntities().get(0);
            diseaseAnnotations.forEach(diseaseAnnotation -> {
                entity.addDisease(diseaseAnnotation.getDisease());
                entity.addPublicationEvidenceCode(diseaseAnnotation.getPrimaryAnnotatedEntities().get(0).getPublicationEvidenceCodes());
            });
            mergedAnnotations.add(entity);
            diseaseAnnotationPureMap.put(geneID, mergedAnnotations);
        }));

        ModelAllianceCacheManager managerModel = new ModelAllianceCacheManager();

        diseaseAnnotationPureMap.forEach((geneID, value) -> {
            JsonResultResponse<PrimaryAnnotatedEntity> result = new JsonResultResponse<>();
            result.setResults(value);
            try {
                if (geneID.equals("MGI:104798")) {
                    log.info("found gene: " + geneID + " with annotations: " + result.getResults().size());
                    //result.getResults().forEach(entity -> log.info(entity.getId()));
                }
                managerModel.putCache(geneID, result, View.PrimaryAnnotation.class, CacheAlliance.GENE_PURE_AGM_DISEASE);
                progressProcess();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private boolean populateAllelesCache(DiseaseRibbonService diseaseRibbonService, Map<String, Set<String>> closureMapping, Set<String> allIDs, DiseaseAllianceCacheManager manager) {
        Set<DiseaseEntityJoin> alleleEntityJoins = diseaseRepository.getAllDiseaseAlleleEntityJoins();

        List<DiseaseAnnotation> alleleList = getDiseaseAnnotationsFromDEJs(alleleEntityJoins, diseaseRibbonService);
        if (alleleList == null)
            return true;

        log.info("Number of DiseaseAnnotation objects with Alleles: " + String.format("%,d", alleleList.size()));
        Map<String, List<DiseaseAnnotation>> diseaseAlleleAnnotationTermMap = alleleList.stream()
                .collect(groupingBy(annotation -> annotation.getDisease().getPrimaryKey()));

        Map<String, List<DiseaseAnnotation>> diseaseAlleleAnnotationMap = new HashMap<>();
        allIDs.forEach(termID -> {
            Set<String> allDiseaseIDs = closureMapping.get(termID);
            List<DiseaseAnnotation> allAnnotations = new ArrayList<>();
            allDiseaseIDs.stream()
                    .filter(id -> diseaseAlleleAnnotationTermMap.get(id) != null)
                    .forEach(id -> allAnnotations.addAll(diseaseAlleleAnnotationTermMap.get(id)));
            diseaseAlleleAnnotationMap.put(termID, allAnnotations);
        });
        diseaseAlleleAnnotationMap.forEach((key, value) -> {
            JsonResultResponseDiseaseAnnotation result = new JsonResultResponseDiseaseAnnotation();
            result.setResults(value);
            try {
                manager.putCache(key, result, View.DiseaseCacher.class, CacheAlliance.DISEASE_ALLELE_ANNOTATION);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        return false;
    }

    private List<DiseaseAnnotation> getDiseaseAnnotationsFromDEJs(Set<DiseaseEntityJoin> joinList, DiseaseRibbonService diseaseRibbonService) {
        return joinList.stream()
                .map(diseaseEntityJoin -> {
                    DiseaseAnnotation document = new DiseaseAnnotation();
                    document.setPrimaryKey(diseaseEntityJoin.getPrimaryKey());
                    document.setGene(diseaseEntityJoin.getGene());
                    document.setFeature(diseaseEntityJoin.getAllele());
                    document.setModel(diseaseEntityJoin.getModel());
                    document.setDisease(diseaseEntityJoin.getDisease());
                    document.setSource(diseaseEntityJoin.getSource());
                    document.setAssociationType(diseaseEntityJoin.getJoinType());
                    document.setSortOrder(diseaseEntityJoin.getSortOrder());
                    List<Gene> orthologyGenes = diseaseEntityJoin.getOrthologyGenes();
                    if (orthologyGenes != null) {
                        orthologyGenes.sort(Comparator.comparing(gene -> gene.getSymbol().toLowerCase()));
                        document.setOrthologyGenes(orthologyGenes);
                    }

                    // used to populate the DOTerm object on the PrimaryAnnotationEntity object
                    // Needed as the same AGM can be reused on multiple pubJoin nodes.
                    Map<String, PrimaryAnnotatedEntity> entities = new HashMap<>();
                    if (CollectionUtils.isNotEmpty(diseaseEntityJoin.getPublicationJoins())) {
                        // create PAEs from AGMs
                        diseaseEntityJoin.getPublicationJoins()
                                .stream()
                                .filter(pubJoin -> pubJoin.getModel() != null)
                                .forEach(pubJoin -> {
                                    AffectedGenomicModel model = pubJoin.getModel();
                                    PrimaryAnnotatedEntity entity = entities.get(model.getPrimaryKey());
                                    if (entity == null) {
                                        entity = new PrimaryAnnotatedEntity();
                                        entity.setId(model.getPrimaryKey());
                                        entity.setName(model.getName());
                                        entity.setUrl(model.getModCrossRefCompleteUrl());
                                        entity.setDisplayName(model.getNameText());
                                        entity.setType(GeneticEntity.getType(model.getSubtype()));
                                        entities.put(model.getPrimaryKey(), entity);
                                    }
                                    document.addPrimaryAnnotatedEntity(entity);
                                    entity.addPublicationEvidenceCode(pubJoin);
                                    entity.addDisease(diseaseEntityJoin.getDisease());
                                });
                        // create PAEs from Alleles
                        diseaseEntityJoin.getPublicationJoins()
                                .stream()
                                .filter(pubJoin -> CollectionUtils.isNotEmpty(pubJoin.getAlleles()))
                                .forEach(pubJoin -> pubJoin.getAlleles().forEach(allele -> {
                                    PrimaryAnnotatedEntity entity = entities.get(allele.getPrimaryKey());
                                    if (entity == null) {
                                        entity = new PrimaryAnnotatedEntity();
                                        entity.setId(allele.getPrimaryKey());
                                        entity.setName(allele.getSymbol());
                                        List<CrossReference> refs = allele.getCrossReferences();
                                        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(refs))
                                            entity.setUrl(refs.get(0).getCrossRefCompleteUrl());

                                        entity.setDisplayName(allele.getSymbolText());
                                        entity.setType(GeneticEntity.CrossReferenceType.ALLELE);
                                        entities.put(allele.getPrimaryKey(), entity);
                                    }
                                    document.addPrimaryAnnotatedEntity(entity);
                                    entity.addPublicationEvidenceCode(pubJoin);
                                    entity.addDisease(diseaseEntityJoin.getDisease());
                                }));
                    }
                    List<PublicationJoin> publicationJoins = diseaseEntityJoin.getPublicationJoins();
                    if (useCache) {
                        diseaseCacheRepository.populatePublicationJoinsFromCache(diseaseEntityJoin.getPublicationJoins());
                    } else {
                        diseaseRepository.populatePublicationJoins(publicationJoins);
                    }
                    document.setPublicationJoins(publicationJoins);
    /*
                        List<ECOTerm> ecoList = diseaseEntityJoin.getPublicationJoins().stream()
                                .filter(join -> CollectionUtils.isNotEmpty(join.getEcoCode()))
                                .map(PublicationJoin::getEcoCode)
                                .flatMap(Collection::stream).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
                        document.setEcoCodes(ecoList.stream().distinct().collect(Collectors.toList()));
    */
                    // work around as I cannot figure out how to include the ECOTerm in the overall query without slowing down the performance.
                    List<ECOTerm> evidences;
                    if (useCache) {
                        evidences = diseaseCacheRepository.getEcoTermsFromCache(diseaseEntityJoin.getPublicationJoins());
                    } else {
                        evidences = diseaseRepository.getEcoTerm(diseaseEntityJoin.getPublicationJoins());
                        Set<String> slimId = diseaseRibbonService.getAllParentIDs(diseaseEntityJoin.getDisease().getPrimaryKey());
                        document.setParentIDs(slimId);
                    }
                    document.setEcoCodes(evidences);
                    progressProcess();
                    return document;
                })
                .collect(toList());
    }

    private void mergeDiseaseAnnotationsByAGM(List<DiseaseAnnotation> allDiseaseAnnotations) {
        Map<DOTerm, Map<Gene, Map<String, List<DiseaseAnnotation>>>> multiIndex = allDiseaseAnnotations.stream()
                .filter(diseaseAnnotation -> diseaseAnnotation.getGene() != null)
                .collect(groupingBy(DiseaseAnnotation::getDisease,
                        groupingBy(DiseaseAnnotation::getGene,
                                groupingBy(DiseaseAnnotation::getAssociationType))));

        multiIndex.forEach((doTerm, geneMapMap) -> {
            geneMapMap.forEach((gene, stringListMap) -> {
                stringListMap.forEach((type, diseaseAnnotations) -> {
                    if (type.equals("is_implicated_in") && diseaseAnnotations.size() > 1) {

                        List<PrimaryAnnotatedEntity> models = diseaseAnnotations.stream()
                                .filter(diseaseAnnotation -> diseaseAnnotation.getPrimaryAnnotatedEntities() != null)
                                .map(DiseaseAnnotation::getPrimaryAnnotatedEntities)
                                .flatMap(Collection::stream)
                                .collect(toList());

                        // only merge annotations that have at least one PAE
/*
                        if (CollectionUtils.isEmpty(models))
                            return;
*/

                        DiseaseAnnotation annotation = diseaseAnnotations.get(0);
                        int index = 0;
                        for (DiseaseAnnotation annot : diseaseAnnotations) {
                            if (index++ == 0)
                                continue;
                            annotation.addAllPrimaryAnnotatedEntities(annot.getPrimaryAnnotatedEntities());
                            annotation.addPublicationJoins(annot.getPublicationJoins());
                            annot.setRemove(true);
                        }
                    }
                });
            });
        });

        allDiseaseAnnotations.removeIf(DiseaseAnnotation::isRemove);
    }

}

