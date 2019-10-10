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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Log4j2
public class DiseaseCacher extends Cacher {

    private static DiseaseRepository diseaseRepository = new DiseaseRepository();

    protected void cache() {

        startProcess("diseaseRepository.getAllDiseaseEntityJoins");

        Set<DiseaseEntityJoin> joinList = diseaseRepository.getAllDiseaseEntityJoins();
        if (joinList == null)
            return;

        finishProcess();

        // grouping orthologous records
        List<DiseaseAnnotation> summaryList = new ArrayList<>();

        DiseaseRibbonService diseaseRibbonService = new DiseaseRibbonService();


        startProcess("diseaseRepository.getAllDiseaseEntityJoins");

        // used to populate the DOTerm object on the PrimaryAnnotationEntity object
        Map<String, PrimaryAnnotatedEntity> entities = new HashMap<>();
        List<DiseaseAnnotation> allDiseaseAnnotations = joinList.stream()
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
                    Gene orthologyGene = diseaseEntityJoin.getOrthologyGene();
                    if (orthologyGene != null) {
                        document.setOrthologyGene(orthologyGene);
                        document.addOrthologousGene(orthologyGene);
                    }

                    if (CollectionUtils.isNotEmpty(diseaseEntityJoin.getPublicationJoins())) {
                        diseaseEntityJoin.getPublicationJoins()
                                .stream()
                                .filter(pubJoin -> CollectionUtils.isNotEmpty(pubJoin.getModels()))
                                .forEach(pubJoin -> {
                                    pubJoin.getModels().forEach(model -> {
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
                                });
                    }
                    document.setPublicationJoins(diseaseRepository.populatePublicationJoins(diseaseEntityJoin.getPublicationJoins()));
                    List<Publication> publicationList = diseaseEntityJoin.getPublicationJoins().stream()
                            .map(PublicationJoin::getPublication).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
                    document.setPublications(publicationList.stream().distinct().collect(Collectors.toList()));

/*
                    List<ECOTerm> ecoList = diseaseEntityJoin.getPublicationJoins().stream()
                            .filter(join -> CollectionUtils.isNotEmpty(join.getEcoCode()))
                            .map(PublicationJoin::getEcoCode)
                            .flatMap(Collection::stream).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
                    document.setEcoCodes(ecoList.stream().distinct().collect(Collectors.toList()));
*/
                    // work around as I cannot figure out how to include the ECOTerm in the overall query without slowing down the performance.
                    List<ECOTerm> evidences = diseaseRepository.getEcoTerm(diseaseEntityJoin.getPublicationJoins());
                    document.setEcoCodes(evidences);
                    Set<String> slimId = diseaseRibbonService.getAllParentIDs(diseaseEntityJoin.getDisease().getPrimaryKey());
                    document.setParentIDs(slimId);
                    progressProcess();
                    return document;
                })
                .collect(toList());

        finishProcess();

        // default sorting
        DiseaseAnnotationSorting sorting = new DiseaseAnnotationSorting();
        allDiseaseAnnotations.sort(sorting.getComparator(SortingField.DEFAULT, Boolean.TRUE));

        int currentHashCode = 0;
        for (DiseaseAnnotation document : allDiseaseAnnotations) {
            int hash = document.hashCode();
            if (currentHashCode == hash) {
                summaryList.get(summaryList.size() - 1).addOrthologousGene(document.getOrthologyGene());
            } else {
                summaryList.add(document);
            }
            currentHashCode = hash;
        }


        log.info("Retrieved " + String.format("%,d", allDiseaseAnnotations.size()) + " annotations");
        Map<String, Set<String>> closureMapping = diseaseRepository.getClosureMapping();
        log.info("Number of Disease IDs: " + closureMapping.size());
        final Set<String> allIDs = closureMapping.keySet();

        long start = System.currentTimeMillis();
        // loop over all disease IDs (termID)
        // and store the annotations in a map for quick retrieval
        Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap = new HashMap<>();

        Map<String, List<DiseaseAnnotation>> diseaseAnnotationTermMap = allDiseaseAnnotations.stream()
                .collect(groupingBy(annotation -> annotation.getDisease().getPrimaryKey()));

        allIDs.forEach(termID -> {
            Set<String> allDiseaseIDs = closureMapping.get(termID);
            List<DiseaseAnnotation> allAnnotations = new ArrayList<>();
            allDiseaseIDs.stream()
                    .filter(id -> diseaseAnnotationTermMap.get(id) != null)
                    .forEach(id -> allAnnotations.addAll(diseaseAnnotationTermMap.get(id)));
            diseaseAnnotationMap.put(termID, allAnnotations);
        });

        log.info("Number of Disease IDs in disease Map: " + diseaseAnnotationMap.size());

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

        diseaseRepository.clearCache();

    }

}

