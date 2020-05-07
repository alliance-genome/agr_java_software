package org.alliancegenome.cacher.cachers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Log4j2
public class ModelCacher extends Cacher {

    private static GeneRepository geneRepository = new GeneRepository();

    @Override
    protected void cache() {

        startProcess("getAllAffectedModelsSTR");

        List<AffectedGenomicModel> modelSTRs = geneRepository.getAllAffectedModelsSTR();
        log.info("Number of STR Models: " + String.format("%,d", modelSTRs.size()));

        List<AffectedGenomicModel> allModels = new ArrayList<>(modelSTRs);

        List<AffectedGenomicModel> models = geneRepository.getAllAffectedModelsAllele();
        log.info("Number of Allele Models: " + String.format("%,d", models.size()));
        allModels.addAll(models);

        List<PrimaryAnnotatedEntity> allEntities = allModels.stream()
                .map(model -> {
                    PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
                    entity.setId(model.getPrimaryKey());
                    entity.setName(model.getName());
                    entity.setUrl(model.getModCrossRefCompleteUrl());
                    entity.setDisplayName(model.getNameText());
                    if (CollectionUtils.isNotEmpty(model.getSequenceTargetingReagents())) {
                        entity.setSequenceTargetingReagents(model.getSequenceTargetingReagents());
                        entity.setSpecies(model.getSequenceTargetingReagents().get(0).getGene().getSpecies());
                    }
                    if (CollectionUtils.isNotEmpty(model.getAlleles())) {
                        entity.setAlleles(model.getAlleles());
                        entity.setSpecies(model.getAlleles().get(0).getSpecies());
                    }
                    if (model.getSubtype() != null)
                        entity.setType(GeneticEntity.CrossReferenceType.getCrossReferenceType(model.getSubtype()));
                    entity.setDataProvider(model.getDataProvider());
                    return entity;
                })
                .collect(Collectors.toList());

        log.info("Number of all PAE: " + String.format("%,d", allEntities.size()));

        Map<String, List<PrimaryAnnotatedEntity>> geneMap = new HashMap<>();

        allEntities.stream()
                .filter(entity -> CollectionUtils.isNotEmpty(entity.getSequenceTargetingReagents()))
                .forEach(entity -> {
                    entity.getSequenceTargetingReagents().forEach(sequenceTargetingReagent -> {
                        List<PrimaryAnnotatedEntity> annotations = geneMap.computeIfAbsent(sequenceTargetingReagent.getGene().getPrimaryKey(), k -> new ArrayList<>());
                        annotations.add(entity);
                    });
                });

        allEntities.stream()
                .filter(entity -> CollectionUtils.isNotEmpty(entity.getAlleles()))
                .forEach(entity -> {
                    entity.getAlleles().forEach(allele -> {
                        List<PrimaryAnnotatedEntity> annotations = geneMap.computeIfAbsent(allele.getGene().getPrimaryKey(), k -> new ArrayList<>());
                        annotations.add(entity);
                    });
                });

        finishProcess();

        if (CollectionUtils.isEmpty(allModels))
            return;

        startProcess("create models and place them into cache: ");

        log.info("Number of Genes with Models: " + String.format("%,d", geneMap.size()));

        populateCacheFromMap(geneMap, View.PrimaryAnnotation.class, CacheAlliance.GENE_ASSOCIATION_MODEL_GENE);

        CacheStatus status = new CacheStatus(CacheAlliance.GENE_ASSOCIATION_MODEL_GENE);
        status.setNumberOfEntityIDs(geneMap.size());
        status.setNumberOfEntities(allEntities.size());

        Map<String, List<Species>> speciesStats = allEntities.stream()
                .filter(annotation -> annotation.getSpecies() != null)
                .map(PrimaryAnnotatedEntity::getSpecies)
                .collect(groupingBy(Species::getName));

        Map<String, Integer> entityStats = new TreeMap<>();
        geneMap.forEach((diseaseID, annotations) -> entityStats.put(diseaseID, annotations.size()));
        populateStatisticsOnStatus(status, entityStats, speciesStats);

        status.setJsonViewClass(View.PrimaryAnnotation.class.getSimpleName());
        status.setCollectionEntity(PrimaryAnnotatedEntity.class.getSimpleName());
        setCacheStatus(status);

        finishProcess();
        geneRepository.clearCache();

    }
}
