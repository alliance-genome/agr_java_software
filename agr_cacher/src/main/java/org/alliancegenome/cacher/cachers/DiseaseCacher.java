package org.alliancegenome.cacher.cachers;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.service.DiseaseRibbonService;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.DiseaseAllianceCacheManager;
import org.alliancegenome.core.service.DiseaseAnnotationSorting;
import org.alliancegenome.core.service.JsonResultResponseDiseaseAnnotation;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Log4j2
public class DiseaseCacher extends Cacher {

    private static DiseaseRepository diseaseRepository = new DiseaseRepository();

    protected void cache() {

        startProcess("diseaseRepository.getAllDiseaseEntityJoins");

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

        Set<DiseaseEntityJoin> alleleEntityJoins = diseaseRepository.getAllDiseaseAlleleEntityJoins();

        List<DiseaseAnnotation> alleleList = getDiseaseAnnotationsFromDEJs(alleleEntityJoins, diseaseRibbonService);
        if (alleleList == null)
            return;

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


        diseaseRepository.clearCache();

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
                                        entity.setUrl(allele.getModCrossRefCompleteUrl());
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
                        diseaseRepository.populatePublicationJoinsFromCache(diseaseEntityJoin.getPublicationJoins());
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
                        evidences = diseaseRepository.getEcoTermsFromCache(diseaseEntityJoin.getPublicationJoins());
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

