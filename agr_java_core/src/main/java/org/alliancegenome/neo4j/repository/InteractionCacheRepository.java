package org.alliancegenome.neo4j.repository;

import org.alliancegenome.core.service.*;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.DecimalFormat;
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
    private static Map<String, List<InteractionGeneJoin>> interactionAnnotationMapGeneA = new HashMap<>();
    // used for filtering and sorting on GeneB
    private static Map<String, List<InteractionGeneJoin>> interactionAnnotationMapGeneB = new HashMap<>();
    private static boolean caching;

    public PaginationResult<InteractionGeneJoin> getInteractionAnnotationList(String geneID, Pagination pagination) {
        checkCache();
        if (caching)
            return null;

        // check geneA map
        List<InteractionGeneJoin> interactionAnnotationListA = interactionAnnotationMapGeneA.get(geneID);
        //filtering
        List<InteractionGeneJoin> filteredInteractionAnnotationListA = filterInteractionAnnotations(interactionAnnotationListA, pagination.getFieldFilterValueMap(), true);

        // check geneB map
        List<InteractionGeneJoin> interactionAnnotationListB = interactionAnnotationMapGeneB.get(geneID);
        //filtering
        List<InteractionGeneJoin> filteredInteractionAnnotationListB = filterInteractionAnnotations(interactionAnnotationListB, pagination.getFieldFilterValueMap(), false);

        List<InteractionGeneJoin> filteredInteractionAnnotationList = new ArrayList<>(filteredInteractionAnnotationListA);
        // create new InteractionGeneJoin objects to be able to sort on the full collection
        List<InteractionGeneJoin> convertedList = filteredInteractionAnnotationListB.parallelStream()
                .map(join -> {
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
                })
                .collect(Collectors.toList());
        filteredInteractionAnnotationList.addAll(convertedList);

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
        // remove GeneA and GeneB elements
        FieldFilter interactorGeneSymbolB = FieldFilter.INTERACTOR_GENE_SYMBOL_B;
        FieldFilter interactorGeneSymbolA = FieldFilter.INTERACTOR_GENE_SYMBOL_A;
        FieldFilter interactorGeneSymbol = FieldFilter.INTERACTOR_GENE_SYMBOL;
        resetFilterMapWithA(fieldFilterValueMap, useGeneAasSource, interactorGeneSymbol, interactorGeneSymbolA, interactorGeneSymbolB);
        resetFilterMapWithA(fieldFilterValueMap, useGeneAasSource, FieldFilter.INTERACTOR_SPECIES, FieldFilter.INTERACTOR_SPECIES_A, FieldFilter.INTERACTOR_SPECIES_B);
        resetFilterMapWithA(fieldFilterValueMap, useGeneAasSource, FieldFilter.MOLECULE_TYPE, FieldFilter.MOLECULE_TYPE_B, FieldFilter.MOLECULE_TYPE_A);
        resetFilterMapWithA(fieldFilterValueMap, useGeneAasSource, FieldFilter.INTERACTOR_MOLECULE_TYPE, FieldFilter.MOLECULE_TYPE_A, FieldFilter.MOLECULE_TYPE_B);

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

    private void resetFilterMapWithA(BaseFilter fieldFilterValueMap, boolean useGeneAasSource, FieldFilter externalFilter, FieldFilter internalFieldA, FieldFilter internalFieldB) {
        fieldFilterValueMap.keySet().removeIf(fieldFilter ->
                fieldFilter.equals(internalFieldA) || fieldFilter.equals(internalFieldB)
        );
        String value = fieldFilterValueMap.get(externalFilter);
        if (value != null) {
            if (useGeneAasSource)
                fieldFilterValueMap.addFieldFilter(internalFieldB, value);
            else
                fieldFilterValueMap.addFieldFilter(internalFieldA, value);
        }
    }


    private void checkCache() {
        if (allInteractionAnnotations == null && !caching) {
            caching = true;
            cacheAllInteractionAnnotations();
            caching = false;
        }
    }

    private void cacheAllInteractionAnnotations() {
        long start = System.currentTimeMillis();
        allInteractionAnnotations = interactionRepository.getAllInteractions();
        int size = allInteractionAnnotations.size();
        DecimalFormat myFormatter = new DecimalFormat("###,###.##");
        System.out.println("Retrieved " + myFormatter.format(size) + " interaction records");
        // replace Gene references with the cached Gene references to keep the memory imprint low.

        // group by gene ID as geneA or GeneB
        interactionAnnotationMapGeneA = allInteractionAnnotations.parallelStream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGeneA().getPrimaryKey()));

        interactionAnnotationMapGeneB = allInteractionAnnotations.parallelStream()
                // exclude self interaction as it is use in the 'A' version already
                .filter(join -> !join.getGeneA().getPrimaryKey().equals(join.getGeneB().getPrimaryKey()))
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGeneB().getPrimaryKey()));

        log.info("Time to create annotation histogram: " + (System.currentTimeMillis() - start) / 1000);
    }

    public List<InteractionGeneJoin> getInteractions(String id, Pagination pagination) {
        return null;
    }
}
