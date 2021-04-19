package org.alliancegenome.cacher.cachers;

import static java.util.stream.Collectors.groupingBy;

import java.util.*;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.alliancegenome.neo4j.view.View;

import lombok.extern.log4j.Log4j2;
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
        //parallelStream is unsafe here, we found out that it lost InteractionGeneJoin.getPhenotypes().getPhenotypeStatement() information, it return null
        //Map<String, List<InteractionGeneJoin>> interactionAnnotationMapGene = allInteractionAnnotations.parallelStream()
        Map<String, List<InteractionGeneJoin>> interactionAnnotationMapGene = allInteractionAnnotations.stream()        
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

        startProcess("add interactions to cache", allInteractionAnnotations.size());

        interactionAnnotationMapGene.forEach((key, value) -> {
            cacheService.putCacheEntry(key, value, View.Interaction.class, CacheAlliance.GENE_INTERACTION);
            progressProcess();
        });

        finishProcess();

        log.info("All interactions: " + allInteractionAnnotations.size());
        log.info("Genes with interactions: " + interactionAnnotationMapGene.size());
        Map<String, Integer> stats = new HashMap<>(interactionAnnotationMapGene.size());
        interactionAnnotationMapGene.forEach((geneID, joins) -> stats.put(geneID, joins.size()));

        Map<String, List<InteractionGeneJoin>> speciesStats = allInteractionAnnotations.stream().collect(groupingBy(join -> join.getGeneA().getSpecies().getName()));

        Map<String, Integer> speciesStatsInt = new HashMap<>();
        speciesStats.forEach((species, joins) -> stats.put(species, joins.size()));

        CacheStatus status = new CacheStatus(CacheAlliance.GENE_INTERACTION);
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
        newJoin.setAlleleA(join.getAlleleB());
        newJoin.setAlleleB(join.getAlleleA());
        newJoin.setPhenotypes(join.getPhenotypes());
        return newJoin;
    }

    @Override
    public void close() {
        interactionRepository.close();
    }

}
