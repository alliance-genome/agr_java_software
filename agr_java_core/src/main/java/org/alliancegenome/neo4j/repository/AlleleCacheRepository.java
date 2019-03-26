package org.alliancegenome.neo4j.repository;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class AlleleCacheRepository {

    public JsonResultResponse<Allele> getAllelesBySpecies(String species, Pagination pagination) {
        checkCache();
        if (caching)
            return null;

        List<Allele> allAlleles = taxonAlleleMap.get(species);
        return getAlleleJsonResultResponse(pagination, allAlleles);
    }

    public JsonResultResponse<Allele> getAllelesByGene(String geneID, Pagination pagination) {
        checkCache();
        if (caching)
            return null;

        List<Allele> allAlleles = geneAlleleMap.get(geneID);
        return getAlleleJsonResultResponse(pagination, allAlleles);
    }

    private List<Allele> getSortedAndPaginatedAlleles(List<Allele> alleleList, Pagination pagination) {
        // sorting
/*
        SortingField sortingField = null;
        String sortBy = pagination.getSortBy();
        if (sortBy != null && !sortBy.isEmpty())
            sortingField = SortingField.getSortingField(sortBy.toUpperCase());

        DiseaseAnnotationSorting sorting = new DiseaseAnnotationSorting();
        alleleList.sort(sorting.getComparator(sortingField, pagination.getAsc()));
*/

        // paginating
        return alleleList.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(Collectors.toList());
    }

    private JsonResultResponse<Allele> getAlleleJsonResultResponse(Pagination pagination, List<Allele> allAlleles) {
        JsonResultResponse<Allele> response = new JsonResultResponse<>();
        response.setResults(getSortedAndPaginatedAlleles(allAlleles, pagination));
        response.setTotal(allAlleles.size());
        return response;
    }

    private Log log = LogFactory.getLog(getClass());
    // cached value
    private static List<Allele> allAlleles = null;
    // Map<gene ID, List<Allele>> grouped by gene ID
    private static Map<String, List<Allele>> geneAlleleMap;
    // Map<taxon ID, List<Allele>> grouped by taxon ID
    private static Map<String, List<Allele>> taxonAlleleMap;

    private static boolean caching;
    private AlleleRepository alleleRepo = new AlleleRepository();

    private void checkCache() {
        if (allAlleles == null && !caching) {
            caching = true;
            cacheAllAlleles();
            caching = false;
        }
    }

    private void cacheAllAlleles() {
        long startTime = System.currentTimeMillis();
        Set<Allele> allAlleleSet = alleleRepo.getAllAlleles();
        if (allAlleleSet == null)
            return;

        allAlleles = new ArrayList<>(allAlleleSet);
        allAlleles.sort(Comparator.comparing(GeneticEntity::getSymbol));
        geneAlleleMap = allAlleles.stream()
                .collect(groupingBy(allele -> allele.getGene().getPrimaryKey()));


        taxonAlleleMap = allAlleles.stream()
                .collect(groupingBy(allele -> allele.getSpecies().getPrimaryKey()));

        log.info("Number of all Alleles: " + allAlleles.size());
        log.info("Number of all Genes with Alleles: " + geneAlleleMap.size());
        printTaxonMap();
        log.info("Time to create cache: " + (System.currentTimeMillis() - startTime) / 1000);

    }

    private void printTaxonMap() {
        log.info("Taxon / Allele map: ");
        StringBuilder builder = new StringBuilder();
        taxonAlleleMap.forEach((key, value) -> builder.append(SpeciesType.fromTaxonId(key).getDisplayName() + ": " + value.size() + ", "));
        log.info(builder.toString());

    }

}
