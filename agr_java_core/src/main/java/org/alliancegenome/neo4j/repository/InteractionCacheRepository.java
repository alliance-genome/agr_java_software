package org.alliancegenome.neo4j.repository;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.core.service.*;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class InteractionCacheRepository {

    private Log log = LogFactory.getLog(getClass());
    private static InteractionRepository interactionRepository = new InteractionRepository();

    // cached value
    private static List<InteractionGeneJoin> allInteractionAnnotations = null;
    // Map<gene ID, List<PhenotypeAnnotation>> including annotations to child terms
    // used for filtering and sorting on GeneA
    private static Map<String, List<InteractionGeneJoin>> interactionAnnotationMapGene = new HashMap<>();
    private static boolean caching;
    private static LocalDateTime start;
    private static LocalDateTime end;

    public PaginationResult<InteractionGeneJoin> getInteractionAnnotationList(String geneID, Pagination pagination) {
        checkCache();
        if (caching)
            return null;

        // check gene map
        List<InteractionGeneJoin> interactionAnnotationList = interactionAnnotationMapGene.get(geneID);
        if (interactionAnnotationList == null)
            return null;
        //filtering
        List<InteractionGeneJoin> filteredInteractionAnnotationList = filterInteractionAnnotations(interactionAnnotationList, pagination.getFieldFilterValueMap(), true);

        PaginationResult<InteractionGeneJoin> result = new PaginationResult<>();
        if (!filteredInteractionAnnotationList.isEmpty()) {
            result.setTotalNumber(filteredInteractionAnnotationList.size());
            result.setResult(getSortedAndPaginatedInteractionAnnotations(pagination, filteredInteractionAnnotationList));
        }
        return result;
    }

    private List<InteractionGeneJoin> getSortedAndPaginatedInteractionAnnotations(Pagination pagination,
                                                                                  List<InteractionGeneJoin> filteredInteractionAnnotationList) {
        // sorting
        SortingField sortingField = null;
        String sortBy = pagination.getSortBy();
        if (sortBy != null && !sortBy.isEmpty())
            sortingField = SortingField.getSortingField(sortBy.toUpperCase());

        InteractionAnnotationSorting sorting = new InteractionAnnotationSorting();
        filteredInteractionAnnotationList.sort(sorting.getComparator(sortingField, pagination.getAsc()));

        // paginating
        return filteredInteractionAnnotationList.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(toList());
    }


    private List<InteractionGeneJoin> filterInteractionAnnotations(List<InteractionGeneJoin> interactionAnnotationList, BaseFilter fieldFilterValueMap, boolean useGeneAasSource) {
        if (interactionAnnotationList == null)
            return null;
        if (fieldFilterValueMap == null)
            return interactionAnnotationList;
        return interactionAnnotationList.stream()
                .filter(annotation -> containsFilterValue(annotation, fieldFilterValueMap, useGeneAasSource))
                .collect(Collectors.toList());
    }

    private boolean containsFilterValue(InteractionGeneJoin annotation, BaseFilter fieldFilterValueMap, boolean useGeneAasSource) {
        // remove entries with null values.
        fieldFilterValueMap.values().removeIf(Objects::isNull);
        Set<Boolean> filterResults = fieldFilterValueMap.entrySet().stream()
                .map((entry) -> {
                    FilterFunction<InteractionGeneJoin, String> filterFunction = InteractionAnnotationFiltering.filterFieldMap.get(entry.getKey());
                    if (filterFunction == null)
                        return null;
                    return filterFunction.containsFilterValue(annotation, entry.getValue());
                })
                .collect(Collectors.toSet());

        return !filterResults.contains(false);
    }

    private void checkCache() {
        if (allInteractionAnnotations == null && !caching) {
            caching = true;
            cacheAllInteractionAnnotations();
            caching = false;
        }
    }

    private void cacheAllInteractionAnnotations() {
        start = LocalDateTime.now();
        long start = System.currentTimeMillis();
        allInteractionAnnotations = interactionRepository.getAllInteractions();
        int size = allInteractionAnnotations.size();
        DecimalFormat myFormatter = new DecimalFormat("###,###.##");
        System.out.println("Retrieved " + myFormatter.format(size) + " interaction records");
        // replace Gene references with the cached Gene references to keep the memory imprint low.

        // group by gene ID with geneA
        interactionAnnotationMapGene = allInteractionAnnotations.parallelStream()
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
        end = LocalDateTime.now();
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

    public List<InteractionGeneJoin> getInteractions(String id, Pagination pagination) {
        return null;
    }

    public CacheStatus getCacheStatus() {
        CacheStatus status = new CacheStatus("Interaction");
        status.setCaching(caching);
        status.setStart(start);
        status.setEnd(end);
        if (allInteractionAnnotations != null)
            status.setNumberOfEntities(allInteractionAnnotations.size());
        return status;
    }

}
