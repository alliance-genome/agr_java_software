package org.alliancegenome.cacher.cachers;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.InteractionAllianceCacheManager;
import org.alliancegenome.core.service.JsonResultResponseInteraction;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.core.JsonProcessingException;

public class InteractionCacher extends Cacher {

    private static InteractionRepository interactionRepository = new InteractionRepository();

    public InteractionCacher() {
        super();
    }

    @Override
    protected void cache() {

        startProcess("interactionRepository.getAllInteractions");

        List<InteractionGeneJoin> allInteractionAnnotations = interactionRepository.getAllInteractions();
        
        finishProcess();

        
        startProcess("interactionAnnotationMapGene", allInteractionAnnotations.size());
        
        Map<String, List<InteractionGeneJoin>> interactionAnnotationMapGene = allInteractionAnnotations.parallelStream()
                // exclude self-interaction
                .filter(interactionGeneJoin -> !interactionGeneJoin.getGeneA().getPrimaryKey().equals(interactionGeneJoin.getGeneB().getPrimaryKey()))
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGeneA().getPrimaryKey()));

        finishProcess();
        
        startProcess("create joins", allInteractionAnnotations.size());
        
        allInteractionAnnotations.forEach(join -> {
            String primaryKey = join.getGeneB().getPrimaryKey();
            List<InteractionGeneJoin> joins = interactionAnnotationMapGene.computeIfAbsent(primaryKey, k -> new ArrayList<>());
            joins.add(createNewInteractionGeneJoin(join));
        });
        
        finishProcess();

        InteractionAllianceCacheManager manager = new InteractionAllianceCacheManager();
        
        startProcess("add interactions to cache", allInteractionAnnotations.size());

        interactionAnnotationMapGene.forEach((key, value) -> {
            JsonResultResponseInteraction result = new JsonResultResponseInteraction();
            result.setResults(value);
            try {
                manager.putCache(key, result, View.Interaction.class, CacheAlliance.GENE_INTERACTION);
                progressProcess();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        
        finishProcess();
        setCacheStatus(allInteractionAnnotations.size(), CacheAlliance.GENE_INTERACTION.getCacheName());

        interactionRepository.clearCache();
    }

    private InteractionGeneJoin createNewInteractionGeneJoin(InteractionGeneJoin join) {
        InteractionGeneJoin newJoin = new InteractionGeneJoin();
        newJoin.setPrimaryKey(join.getPrimaryKey());
        newJoin.setJoinType(join.getJoinType());
        newJoin.setAggregationDatabase(join.getAggregationDatabase());
        newJoin.setCrossReferences(join.getCrossReferences());
        newJoin.setDetectionsMethods(join.getDetectionsMethods());
        newJoin.setGeneA(join.getGeneB());
        newJoin.setGeneB(join.getGeneA());
        newJoin.setInteractionType(join.getInteractionType());
        newJoin.setInteractorARole(join.getInteractorBRole());
        newJoin.setInteractorAType(join.getInteractorBType());
        newJoin.setInteractorBRole(join.getInteractorARole());
        newJoin.setInteractorBType(join.getInteractorAType());
        newJoin.setPublication(join.getPublication());
        newJoin.setSourceDatabase(join.getSourceDatabase());
        newJoin.setId(join.getId());
        return newJoin;
    }

}
