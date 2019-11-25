package org.alliancegenome.cacher.cachers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

@Log4j2
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

        BasicCachingManager manager = new BasicCachingManager();

        startProcess("add interactions to cache", allInteractionAnnotations.size());

        interactionAnnotationMapGene.forEach((key, value) -> {
            manager.setCache(key, value, View.Interaction.class, CacheAlliance.GENE_INTERACTION);
            progressProcess();
        });

        finishProcess();

        log.info("All interactions: " + allInteractionAnnotations.size());
        log.info("Genes with interactions: " + interactionAnnotationMapGene.size());
        Map<String, Integer> stats = new HashMap<>(interactionAnnotationMapGene.size());
        interactionAnnotationMapGene.forEach((geneID, joins) -> {
            stats.put(geneID, joins.size());
        });

        Map<String, List<InteractionGeneJoin>> speciesStats = allInteractionAnnotations.stream().collect(groupingBy(join -> join.getGeneA().getSpecies().getName()));

        Map<String, Integer> speciesStatsInt = new HashMap<>();
        speciesStats.forEach((species, joins) -> {
            stats.put(species, joins.size());
        });

        CacheStatus status = new CacheStatus(CacheAlliance.GENE_INTERACTION.getCacheName());
        status.setNumberOfEntities(allInteractionAnnotations.size());
        status.setEntityStats(stats);
        status.setSpeciesStats(speciesStatsInt);
        setCacheStatus(status);

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
