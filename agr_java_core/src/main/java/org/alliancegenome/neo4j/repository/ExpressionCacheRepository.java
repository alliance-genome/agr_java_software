package org.alliancegenome.neo4j.repository;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.cache.AllianceCacheManager;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.core.service.*;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class ExpressionCacheRepository {


    private static List<String> parentTermIDs = new ArrayList<>();

    static {
        // anatomical entity
        parentTermIDs.add("UBERON:0001062");
        // processual entity stage
        parentTermIDs.add("UBERON:0000000");
        // cellular Component
        parentTermIDs.add("GO:0005575");
    }

    public PaginationResult<ExpressionDetail> getExpressionAnnotations(List<String> geneIDs, String termID, Pagination pagination) {

        List<ExpressionDetail> fullExpressionAnnotationList = new ArrayList<>();
        geneIDs.stream()
                .filter(geneID -> AllianceCacheManager.getCacheSpaceWeb(CacheAlliance.EXPRESSION).get(geneID) != null)
                .forEach(geneID -> fullExpressionAnnotationList.addAll(AllianceCacheManager.getCacheSpaceWeb(CacheAlliance.EXPRESSION).get(geneID)));

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

    private static List<String> parentTermIDs = new ArrayList<>();

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
        return CollectionUtils.isNotEmpty(AllianceCacheManager.getCacheSpaceWeb(CacheAlliance.EXPRESSION).get(geneID));
    }
}
