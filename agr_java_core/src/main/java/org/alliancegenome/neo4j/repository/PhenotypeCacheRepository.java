package org.alliancegenome.neo4j.repository;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.core.service.FilterFunction;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.core.service.PhenotypeAnnotationFiltering;
import org.alliancegenome.core.service.PhenotypeAnnotationSorting;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.PhenotypeEntityJoin;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PhenotypeCacheRepository {

    private Log log = LogFactory.getLog(getClass());
    private static PhenotypeRepository phenotypeRepository = new PhenotypeRepository();
    private GeneCacheRepository geneCacheRepository = new GeneCacheRepository();


    // cached value
    private static List<PhenotypeAnnotation> allPhenotypeAnnotations = null;
    // Map<gene ID, List<PhenotypeAnnotation>> including annotations to child terms
    private static Map<String, List<PhenotypeAnnotation>> phenotypeAnnotationMap = new HashMap<>();
    private static boolean caching;

    public PaginationResult<PhenotypeAnnotation> getPhenotypeAnnotationList(String geneID, Pagination pagination) {
        checkCache();
        if (caching)
            return null;

        List<PhenotypeAnnotation> fullPhenotypeAnnotationList = phenotypeAnnotationMap.get(geneID);

        //filtering
        List<PhenotypeAnnotation> filteredPhenotypeAnnotationList = filterDiseaseAnnotations(fullPhenotypeAnnotationList, pagination.getFieldFilterValueMap());

        PaginationResult<PhenotypeAnnotation> result = new PaginationResult<>();
        result.setTotalNumber(fullPhenotypeAnnotationList.size());
        result.setResult(getSortedAndPaginatedDiseaseAnnotations(pagination, filteredPhenotypeAnnotationList));
        return result;
    }

    private List<PhenotypeAnnotation> getSortedAndPaginatedDiseaseAnnotations(Pagination pagination, List<PhenotypeAnnotation> fullDiseaseAnnotationList) {
        // sorting
        SortingField sortingField = null;
        String sortBy = pagination.getSortBy();
        if (sortBy != null && !sortBy.isEmpty())
            sortingField = SortingField.getSortingField(sortBy.toUpperCase());

        PhenotypeAnnotationSorting sorting = new PhenotypeAnnotationSorting();
        fullDiseaseAnnotationList.sort(sorting.getComparator(sortingField, pagination.getAsc()));

        // paginating
        return fullDiseaseAnnotationList.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(toList());
    }


    private List<PhenotypeAnnotation> filterDiseaseAnnotations(List<PhenotypeAnnotation> phenotypeAnnotationList, BaseFilter fieldFilterValueMap) {
        if (phenotypeAnnotationList == null)
            return null;
        if (fieldFilterValueMap == null)
            return phenotypeAnnotationList;
        return phenotypeAnnotationList.stream()
                .filter(annotation -> containsFilterValue(annotation, fieldFilterValueMap))
                .collect(Collectors.toList());
    }

    private boolean containsFilterValue(PhenotypeAnnotation annotation, BaseFilter fieldFilterValueMap) {
        // remove entries with null values.
        fieldFilterValueMap.values().removeIf(Objects::isNull);

        Set<Boolean> filterResults = fieldFilterValueMap.entrySet().stream()
                .map((entry) -> {
                    FilterFunction<PhenotypeAnnotation, String> filterFunction = PhenotypeAnnotationFiltering.filterFieldMap.get(entry.getKey());
                    if (filterFunction == null)
                        return null;
                    return filterFunction.containsFilterValue(annotation, entry.getValue());
                })
                .collect(Collectors.toSet());

        return !filterResults.contains(false);
    }


    private void checkCache() {
        if (allPhenotypeAnnotations == null && !caching) {
            caching = true;
            cacheAllPhenotypeAnnotations();
            caching = false;
        }
    }

    private void cacheAllPhenotypeAnnotations() {
        long start = System.currentTimeMillis();
        List<PhenotypeEntityJoin> joinList = phenotypeRepository.getAllPhenotypeAnnotations();
        int size = joinList.size();
        DecimalFormat myFormatter = new DecimalFormat("###,###.##");
        System.out.println("Retrieved " + myFormatter.format(size) + " phenotype records");
        // replace Gene references with the cached Gene references to keep the memory imprint low.
        allPhenotypeAnnotations = joinList.stream()
                .map(phenotypeEntityJoin -> {
                    PhenotypeAnnotation document = new PhenotypeAnnotation();
///                    document.setGeneticEntity(geneCacheRepository.getGene(phenotypeEntityJoin.getGene().getPrimaryKey()));
                    document.setGeneticEntity(phenotypeEntityJoin.getGene());
                    document.setPhenotype(phenotypeEntityJoin.getPhenotype().getPhenotypeStatement());
                    return document;
                })
                .collect(toList());

        // group by gene IDs
        phenotypeAnnotationMap = allPhenotypeAnnotations.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGeneticEntity().getPrimaryKey()));

        // default sorting
/*
        DiseaseAnnotationSorting sorting = new DiseaseAnnotationSorting();
        allPhenotypeAnnotations.sort(sorting.getDefaultComparator());
        log.info("Retrieved " + allPhenotypeAnnotations.size() + " annotations");
        long startCreateHistogram = System.currentTimeMillis();
        Map<String, Set<String>> closureMapping = phenotypeRepository.getClosureMapping();
        log.info("Number of Disease IDs: " + closureMapping.size());
        final Set<String> allIDs = closureMapping.keySet();
*/
        log.info("Time to create annotation histogram: " + (System.currentTimeMillis() - start) / 1000);
    }

}
