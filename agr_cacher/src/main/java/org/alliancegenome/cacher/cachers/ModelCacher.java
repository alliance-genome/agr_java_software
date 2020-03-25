package org.alliancegenome.cacher.cachers;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.View;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ModelCacher extends Cacher {

    private static GeneRepository geneRepository = new GeneRepository();

    @Override
    protected void cache() {

        startProcess("getAllAffectedModelsSTR");

        List<AffectedGenomicModel> modelSTRs = geneRepository.getAllAffectedModelsSTR();
        log.info("Number of STR Models: " + String.format("%,d", modelSTRs.size()));
        List<PrimaryAnnotatedEntity> strEntities = modelSTRs.stream()
                .map(model -> {
                    PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
                    entity.setId(model.getPrimaryKey());
                    entity.setName(model.getName());
                    entity.setUrl(model.getModCrossRefCompleteUrl());
                    entity.setDisplayName(model.getNameText());
                    entity.setSequenceTargetingReagents(model.getSequenceTargetingReagents());
                    if (model.getSubtype() != null)
                        entity.setType(GeneticEntity.CrossReferenceType.getCrossReferenceType(model.getSubtype()));
                    entity.setSpecies(model.getSequenceTargetingReagents().get(0).getGene().getSpecies());
                    return entity;
                })
                .collect(Collectors.toList());

        Map<String, List<PrimaryAnnotatedEntity>> geneMap = new HashMap<>();

        strEntities.stream()
                .filter(entity -> entity.getSequenceTargetingReagents() != null)
                .forEach(entity -> {
                    entity.getSequenceTargetingReagents().forEach(sequenceTargetingReagent -> {
                        List<PrimaryAnnotatedEntity> annotations = geneMap.computeIfAbsent(sequenceTargetingReagent.getGene().getPrimaryKey(), k -> new ArrayList<>());
                        annotations.add(entity);
                    });
                });


        List<AffectedGenomicModel> models = geneRepository.getAllAffectedModelsAllele();

        finishProcess();

        if (models == null)
            return;

        startProcess("create models and place them into cache: ");
        log.info("Number of Allele Models: " + String.format("%,d", models.size()));

        List<PrimaryAnnotatedEntity> entities = models.stream()
                .map(model -> {
                    PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
                    entity.setId(model.getPrimaryKey());
                    entity.setName(model.getName());
                    entity.setDisplayName(model.getNameText());
                    entity.setUrl(model.getModCrossRefCompleteUrl());
                    entity.setAlleles(model.getAlleles());
                    entity.setSpecies(model.getAlleles().get(0).getGene().getSpecies());
                    if (model.getSubtype() != null)
                        entity.setType(GeneticEntity.CrossReferenceType.getCrossReferenceType(model.getSubtype()));
                    return entity;
                })
                .collect(Collectors.toList());

        entities.forEach(entity -> {
            entity.getAlleles().forEach(allele -> {
                List<PrimaryAnnotatedEntity> annotations = geneMap.computeIfAbsent(allele.getGene().getPrimaryKey(), k -> new ArrayList<>());
                annotations.add(entity);
            });
        });

        log.info("Number of Genes with Models: " + String.format("%,d", geneMap.size()));

        populateCacheFromMap(geneMap, View.PrimaryAnnotation.class, CacheAlliance.GENE_MODEL);

        CacheStatus status = new CacheStatus(CacheAlliance.GENE_MODEL);
        status.setNumberOfEntities(entities.size());

        Map<String, List<Species>> speciesStats = entities.stream()
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
