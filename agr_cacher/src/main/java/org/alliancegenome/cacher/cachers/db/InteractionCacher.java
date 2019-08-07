package org.alliancegenome.cacher.cachers.db;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.cache.AllianceCacheManager;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cacher.cachers.Cacher;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.ehcache.Cache;

import java.text.DecimalFormat;
import java.util.ArrayList;
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
        long start = System.currentTimeMillis();
        List<InteractionGeneJoin> allInteractionAnnotations = interactionRepository.getAllInteractions();
        int size = allInteractionAnnotations.size();
        DecimalFormat myFormatter = new DecimalFormat("###,###.##");
        System.out.println("Retrieved " + myFormatter.format(size) + " interaction records");
        // replace Gene references with the cached Gene references to keep the memory imprint low.

        // group by gene ID with geneA
        Map<String, List<InteractionGeneJoin>> interactionAnnotationMapGene = allInteractionAnnotations.parallelStream()
                // exclude self-interaction
                .filter(interactionGeneJoin -> !interactionGeneJoin.getGeneA().getPrimaryKey().equals(interactionGeneJoin.getGeneB().getPrimaryKey()))
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGeneA().getPrimaryKey()));

        // add to grouping with geneB as a reference
        // this includes self-interaction
        allInteractionAnnotations.forEach(join -> {
            String primaryKey = join.getGeneB().getPrimaryKey();
            List<InteractionGeneJoin> joins = interactionAnnotationMapGene.computeIfAbsent(primaryKey, k -> new ArrayList<>());
            joins.add(createNewInteractionGeneJoin(join));
        });
        log.info("Number of gene with interactions: " + interactionAnnotationMapGene.size());
        log.info("Time to create annotation histogram: " + (System.currentTimeMillis() - start) / 1000);
        interactionRepository.clearCache();

        Cache<String, ArrayList> phenotypeCache = AllianceCacheManager.getCacheSpace(CacheAlliance.INTERACTION);
        interactionAnnotationMapGene.forEach((key, value) -> phenotypeCache.put(key, new ArrayList<>(value)));
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
