package org.alliancegenome.cacher.cachers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class ModelCacher extends Cacher {

    private static GeneRepository geneRepository = new GeneRepository();

    @Override
    protected void cache() {

        startProcess("geneRepository.getAllAffectedModelsAllele");

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

        BasicCachingManager manager = new BasicCachingManager();

        geneMap.forEach((geneID, annotations) -> {

            JsonResultResponse<PrimaryAnnotatedEntity> result = new JsonResultResponse<>();
            result.setResults(new ArrayList<>(annotations));
            manager.setCache(geneID, annotations, View.PrimaryAnnotation.class, CacheAlliance.GENE_MODEL);
        });

        finishProcess();
        //setCacheStatus(modeles.size(), CacheAlliance.GENE_ORTHOLOGY.getCacheName());
        geneRepository.clearCache();

    }
}
