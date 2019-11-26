package org.alliancegenome.cache.repository;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;
import org.alliancegenome.core.service.*;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.BaseFilter;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Log4j2
public class InteractionCacheRepository {

    public PaginationResult<InteractionGeneJoin> getInteractionAnnotationList(String geneID, Pagination pagination) {
        // check gene map
        BasicCachingManager<InteractionGeneJoin> manager = new BasicCachingManager<>(InteractionGeneJoin.class);
        List<InteractionGeneJoin> interactionAnnotationList = manager.getCache(geneID, CacheAlliance.GENE_INTERACTION);
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

}
