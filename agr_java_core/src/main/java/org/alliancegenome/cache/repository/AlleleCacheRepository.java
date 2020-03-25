package org.alliancegenome.cache.repository;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.CacheService;
import org.alliancegenome.cache.repository.helper.AlleleFiltering;
import org.alliancegenome.cache.repository.helper.AlleleSorting;
import org.alliancegenome.cache.repository.helper.FilterFunction;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.cache.repository.helper.SortingField;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.collections.CollectionUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RequestScoped
public class AlleleCacheRepository {

    @Inject
    private CacheService cacheService;

    public JsonResultResponse<Allele> getAllelesBySpecies(String taxonID, Pagination pagination) {
        List<Allele> allAlleles = cacheService.getCacheEntries(taxonID, CacheAlliance.ALLELE_SPECIES, Allele.class);
        if (CollectionUtils.isEmpty(allAlleles)) {
            return JsonResultResponse.getEmptyInstance();
        }
        return getAlleleJsonResultResponse(pagination, allAlleles);
    }

    public JsonResultResponse<Allele> getAllelesByGene(String geneID, Pagination pagination) {
        List<Allele> allAlleles = cacheService.getCacheEntries(geneID, CacheAlliance.ALLELE_GENE, Allele.class);
        if (allAlleles == null)
            return null;
        return getAlleleJsonResultResponse(pagination, allAlleles);
    }

    private List<Allele> getSortedAndPaginatedAlleles(List<Allele> alleleList, Pagination pagination) {
        // sorting
        SortingField sortingField = null;
        String sortBy = pagination.getSortBy();
        if (sortBy != null && !sortBy.isEmpty())
            sortingField = SortingField.getSortingField(sortBy.toUpperCase());

        AlleleSorting sorting = new AlleleSorting();
        alleleList.sort(sorting.getComparator(sortingField, pagination.getAsc()));

        // paginating
        return alleleList.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(Collectors.toList());
    }

    private JsonResultResponse<Allele> getAlleleJsonResultResponse(Pagination pagination, List<Allele> allAlleles) {
        JsonResultResponse<Allele> response = new JsonResultResponse<>();

        //filtering
        List<Allele> filteredAlleleList = filterDiseaseAnnotations(allAlleles, pagination.getFieldFilterValueMap());
        response.setResults(getSortedAndPaginatedAlleles(filteredAlleleList, pagination));
        response.setTotal(filteredAlleleList.size());
        return response;
    }

    private List<Allele> filterDiseaseAnnotations(List<Allele> alleleList, BaseFilter fieldFilterValueMap) {
        if (alleleList == null)
            return null;
        if (fieldFilterValueMap == null)
            return alleleList;
        return alleleList.stream()
                .filter(annotation -> containsFilterValue(annotation, fieldFilterValueMap))
                .collect(Collectors.toList());
    }

    private boolean containsFilterValue(Allele allele, BaseFilter fieldFilterValueMap) {
        // remove entries with null values.
        fieldFilterValueMap.values().removeIf(Objects::isNull);

        Set<Boolean> filterResults = fieldFilterValueMap.entrySet().stream()
                .map((entry) -> {
                    FilterFunction<Allele, String> filterFunction = AlleleFiltering.filterFieldMap.get(entry.getKey());
                    if (filterFunction == null)
                        return null;
                    return filterFunction.containsFilterValue(allele, entry.getValue());
                })
                .collect(Collectors.toSet());

        return !filterResults.contains(false);
    }

    private void printTaxonMap() {
        log.info("Taxon / Allele map: ");
        StringBuilder builder = new StringBuilder();
        // TODO taxonAlleleMap.forEach((key, value) -> builder.append(SpeciesType.fromTaxonId(key).getDisplayName() + ": " + value.size() + ", "));
        log.info(builder.toString());

    }

    public List<PhenotypeAnnotation> getPhenotype(String alleleId) {
        //BasicCachingManager<PhenotypeAnnotation> manager = new BasicCachingManager<>(PhenotypeAnnotation.class);
        List<PhenotypeAnnotation> phenotypeAnnotations = cacheService.getCacheEntries(alleleId, CacheAlliance.ALLELE_PHENOTYPE, PhenotypeAnnotation.class);
        if (phenotypeAnnotations == null)
            return null;
        return phenotypeAnnotations;
    }

    public List<DiseaseAnnotation> getDisease(String alleleId) {

        List<DiseaseAnnotation> diseaseAnnotations = cacheService.getCacheEntries(alleleId, CacheAlliance.ALLELE_DISEASE, DiseaseAnnotation.class);
        if (diseaseAnnotations == null)
            return null;
        return diseaseAnnotations;
    }
}
