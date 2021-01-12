package org.alliancegenome.cache.repository;

import static java.util.stream.Collectors.toList;

import java.util.*;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.service.*;
import org.alliancegenome.cache.*;
import org.alliancegenome.cache.repository.helper.*;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.BaseFilter;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RequestScoped
public class InteractionCacheRepository {

    @Inject
    private CacheService cacheService;

    public PaginationResult<InteractionGeneJoin> getInteractionAnnotationList(String geneID, Pagination pagination) {

        List<InteractionGeneJoin> interactionAnnotationList = cacheService.getCacheEntries(geneID, CacheAlliance.GENE_INTERACTION);
        if (interactionAnnotationList == null)
            return null;

        PaginationResult<InteractionGeneJoin> result = new PaginationResult<>();

        FilterService<InteractionGeneJoin> filterService = new FilterService<>(new InteractionAnnotationFiltering());
        ColumnFieldMapping<InteractionGeneJoin> mapping = new InteractionColumnFieldMapping();
        result.setDistinctFieldValueMap(filterService.getDistinctFieldValues(interactionAnnotationList, mapping.getSingleValuedFieldColumns(Table.INTERACTION), mapping));

        //filtering
        List<InteractionGeneJoin> filteredInteractionAnnotationList = filterInteractionAnnotations(interactionAnnotationList, pagination.getFieldFilterValueMap(), true);

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
