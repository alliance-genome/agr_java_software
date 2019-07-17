package org.alliancegenome.neo4j.repository;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.AllianceCacheManager;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.service.FilterFunction;
import org.alliancegenome.core.service.InteractionAnnotationFiltering;
import org.alliancegenome.core.service.InteractionAnnotationSorting;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
        // check gene map
        List<InteractionGeneJoin> interactionAnnotationList = AllianceCacheManager.getCacheSpaceWeb(CacheAlliance.INTERACTION).get(geneID);
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
