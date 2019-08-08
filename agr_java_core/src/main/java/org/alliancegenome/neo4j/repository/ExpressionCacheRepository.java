package org.alliancegenome.neo4j.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.cache.ExpressionAllianceCacheManager;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.core.service.ExpressionAnnotationFiltering;
import org.alliancegenome.core.service.ExpressionAnnotationSorting;
import org.alliancegenome.core.service.FilterFunction;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections4.CollectionUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ExpressionCacheRepository {


    private static List<String> parentTermIDs = new ArrayList<>();
    private ExpressionAllianceCacheManager manager = new ExpressionAllianceCacheManager();

    public static final String UBERON_ANATOMY_ROOT = "UBERON:0001062";

    public static final String UBERON_STAGE_ROOT = "UBERON:0000000";

    public static final String GO_CC_ROOT = "GO:0005575";

    static {
        // anatomical entity
        parentTermIDs.add(UBERON_ANATOMY_ROOT);
        // processual entity stage
        parentTermIDs.add(UBERON_STAGE_ROOT);
        // cellular Component
        parentTermIDs.add(GO_CC_ROOT);
    }

    public PaginationResult<ExpressionDetail> getExpressionAnnotations(List<String> geneIDs, String termID, Pagination pagination) {

        List<ExpressionDetail> fullExpressionAnnotationList = new ArrayList<>();
        geneIDs.stream()
                .filter(geneID -> manager.getExpressions(geneID, View.Expression.class) != null)
                .forEach(geneID -> fullExpressionAnnotationList.addAll(manager.getExpressions(geneID, View.Expression.class)));

        //filtering
        // filter on termID
        List<ExpressionDetail> filterTermIDList = fullExpressionAnnotationList;
        if (termID != null) {
            filterTermIDList = fullExpressionAnnotationList.stream()
                    .filter(expressionDetail -> expressionDetail.getTermIDs().contains(termID))
                    .collect(Collectors.toList());
        }
        List<ExpressionDetail> filteredExpressionAnnotationList = filterExpressionAnnotations(filterTermIDList, pagination.getFieldFilterValueMap());

        PaginationResult<ExpressionDetail> result = new PaginationResult<>();
        if (!filteredExpressionAnnotationList.isEmpty()) {
            result.setTotalNumber(filteredExpressionAnnotationList.size());
            result.setResult(getSortedAndPaginatedExpressions(filteredExpressionAnnotationList, pagination));
        }
        return result;
    }

    private List<ExpressionDetail> filterExpressionAnnotations(List<ExpressionDetail> expressionDetails, BaseFilter fieldFilterValueMap) {
        if (expressionDetails == null)
            return null;
        if (fieldFilterValueMap == null)
            return expressionDetails;
        return expressionDetails.stream()
                .filter(annotation -> containsFilterValue(annotation, fieldFilterValueMap))
                .collect(Collectors.toList());
    }

    private boolean containsFilterValue(ExpressionDetail annotation, BaseFilter fieldFilterValueMap) {
        // remove entries with null values.
        fieldFilterValueMap.values().removeIf(Objects::isNull);
        Set<Boolean> filterResults = fieldFilterValueMap.entrySet().stream()
                .map((entry) -> {
                    FilterFunction<ExpressionDetail, String> filterFunction = ExpressionAnnotationFiltering.filterFieldMap.get(entry.getKey());
                    if (filterFunction == null)
                        return null;
                    return filterFunction.containsFilterValue(annotation, entry.getValue());
                })
                .collect(Collectors.toSet());

        return !filterResults.contains(false);
    }

    private List<ExpressionDetail> getSortedAndPaginatedExpressions(List<ExpressionDetail> expressionList, Pagination pagination) {
        // sorting
        SortingField sortingField = null;
        String sortBy = pagination.getSortBy();
        if (sortBy != null && !sortBy.isEmpty())
            sortingField = SortingField.getSortingField(sortBy.toUpperCase());

        ExpressionAnnotationSorting sorting = new ExpressionAnnotationSorting();
        expressionList.sort(sorting.getComparator(sortingField, pagination.getAsc()));

        // paginating
        return expressionList.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(Collectors.toList());
    }

    private Set<String> getParentTermIDs(String id) {
        return getParentTermIDs(Collections.singletonList(id));
    }

    private Set<String> getParentTermIDs(List<String> aoList) {
        if (aoList == null || aoList.isEmpty())
            return null;
        DiseaseRepository repository = new DiseaseRepository();
        Set<String> parentSet = new HashSet<>(4);
        Map<String, Set<String>> map = repository.getClosureMappingUberon();
        aoList.forEach(id -> {
            parentTermIDs.forEach(parentTermID -> {
                if (map.get(parentTermID) != null && map.get(parentTermID).contains(id))
                    parentSet.add(parentTermID);
            });
            if (id.equals("UBERON:AnatomyOtherLocation"))
                parentSet.add(parentTermIDs.get(0));
            if (id.equals("UBERON:PostEmbryonicPreAdult"))
                parentSet.add(parentTermIDs.get(1));
        });
        return parentSet;
    }

    private Set<String> getGOParentTermIDs(List<String> goList) {
        if (goList == null || goList.isEmpty())
            return null;
        DiseaseRepository repository = new DiseaseRepository();
        Set<String> parentSet = new HashSet<>(4);
        Map<String, Set<String>> map = repository.getClosureMappingGO();
        goList.forEach(id -> {
            parentTermIDs.forEach(parentTermID -> {
                if (map.get(parentTermID) != null && map.get(parentTermID).contains(id))
                    parentSet.add(parentTermID);
            });
            if (id.equals("GO:otherLocations"))
                parentSet.add(parentTermIDs.get(2));
        });
        return parentSet;
    }

    public boolean hasExpression(String geneID) {
        return CollectionUtils.isNotEmpty(manager.getExpressions(geneID, View.Expression.class));
    }
}
