package org.alliancegenome.cache.repository;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.api.service.AlleleColumnFieldMapping;
import org.alliancegenome.api.service.ColumnFieldMapping;
import org.alliancegenome.api.service.FilterService;
import org.alliancegenome.api.service.Table;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.CacheService;
import org.alliancegenome.cache.repository.helper.*;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.apache.commons.collections.CollectionUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RequestScoped
public class AlleleCacheRepository {

    @Inject
    private CacheService cacheService;

    public JsonResultResponse<Allele> getAllelesBySpecies(String taxonID, Pagination pagination) {
        List<Allele> allAlleles = cacheService.getCacheEntries(taxonID, CacheAlliance.ALLELE_SPECIES);
        if (CollectionUtils.isEmpty(allAlleles)) {
            return JsonResultResponse.getEmptyInstance();
        }
        return getAlleleJsonResultResponse(pagination, allAlleles);
    }

    public JsonResultResponse<Allele> getAllelesByGene(String geneID, Pagination pagination) {
        List<Allele> allAlleles = cacheService.getCacheEntries(geneID, CacheAlliance.ALLELE_GENE);
        if (allAlleles == null)
            return null;
        return getAlleleJsonResultResponse(pagination, allAlleles);
    }

    public JsonResultResponse<AlleleVariantSequence> getAllelesAndVariantsByGene(String geneID, Pagination pagination) {
        List<AlleleVariantSequence> allAlleles = cacheService.getCacheEntries(geneID, CacheAlliance.ALLELE_VARIANT_SEQUENCE_GENE);
        if (allAlleles == null)
            return null;
        return getAlleleAndVariantJsonResultResponse(pagination, allAlleles);
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
        FilterService<Allele> filterService = new FilterService<>(new AlleleFiltering());
        List<Allele> filteredAlleleList = filterService.filterAnnotations(allAlleles, pagination.getFieldFilterValueMap());
        response.setResults(getSortedAndPaginatedAlleles(filteredAlleleList, pagination));
        response.setTotal(filteredAlleleList.size());

        // add distinct values
        ColumnFieldMapping<Allele> mapping = new AlleleColumnFieldMapping();
        response.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(allAlleles,
                mapping.getSingleValuedFieldColumns(Table.ALLELE_GENE), mapping));

        return response;
    }

    private JsonResultResponse<AlleleVariantSequence> getAlleleAndVariantJsonResultResponse(Pagination pagination, List<AlleleVariantSequence> allAlleles) {
        JsonResultResponse<AlleleVariantSequence> response = new JsonResultResponse<>();

        //filtering
        FilterService<AlleleVariantSequence> filterService = new FilterService<>(new AlleleVariantSequenceFiltering());
        List<AlleleVariantSequence> filteredAlleleList = filterService.filterAnnotations(allAlleles, pagination.getFieldFilterValueMap());
        response.setResults(filterService.getSortedAndPaginatedAnnotations(pagination, filteredAlleleList, null));
        response.setTotal(filteredAlleleList.size());

        // add distinct values
/*
        ColumnFieldMapping<Allele> mapping = new AlleleColumnFieldMapping();
        response.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(allAlleles,
                mapping.getSingleValuedFieldColumns(Table.ALLELE_GENE), mapping));
*/

        return response;
    }


    private void printTaxonMap() {
        log.info("Taxon / Allele map: ");
        StringBuilder builder = new StringBuilder();
        // TODO taxonAlleleMap.forEach((key, value) -> builder.append(SpeciesType.fromTaxonId(key).getDisplayName() + ": " + value.size() + ", "));
        log.info(builder.toString());

    }

    public List<PhenotypeAnnotation> getPhenotype(String alleleId) {
        //BasicCachingManager<PhenotypeAnnotation> manager = new BasicCachingManager<>(PhenotypeAnnotation.class);
        List<PhenotypeAnnotation> phenotypeAnnotations = cacheService.getCacheEntries(alleleId, CacheAlliance.ALLELE_PHENOTYPE);
        if (phenotypeAnnotations == null)
            return null;
        return phenotypeAnnotations;
    }

    public List<DiseaseAnnotation> getDisease(String alleleId) {

        List<DiseaseAnnotation> diseaseAnnotations = cacheService.getCacheEntries(alleleId, CacheAlliance.ALLELE_DISEASE);
        if (diseaseAnnotations == null)
            return null;
        return diseaseAnnotations;
    }
}
