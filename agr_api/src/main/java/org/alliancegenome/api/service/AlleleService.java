package org.alliancegenome.api.service;

import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.cache.repository.AlleleCacheRepository;
import org.alliancegenome.cache.repository.helper.*;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.*;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.apache.commons.collections.CollectionUtils;

@RequestScoped
public class AlleleService {

    private AlleleRepository alleleRepo = new AlleleRepository();

    @Inject
    private AlleleCacheRepository alleleCacheRepo;

    public Allele getById(String id) {
        return alleleRepo.getAllele(id);
    }

    public JsonResultResponse<Allele> getAllelesBySpecies(String species, Pagination pagination) {
        String taxon = SpeciesType.getTaxonId(species);
        return alleleCacheRepo.getAllelesBySpecies(taxon, pagination);
    }

    public JsonResultResponse<Allele> getAllelesByGene(String geneID, Pagination pagination) {
        return alleleCacheRepo.getAllelesByGene(geneID, pagination);
    }

    public JsonResultResponse<PhenotypeAnnotation> getPhenotype(String id, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();

        List<PhenotypeAnnotation> annotations = alleleCacheRepo.getPhenotype(id);

        JsonResultResponse<PhenotypeAnnotation> result = new JsonResultResponse<>();

        FilterService<PhenotypeAnnotation> filterService = new FilterService<>(new PhenotypeAnnotationFiltering());
        if (CollectionUtils.isNotEmpty(annotations)) {
            List<PhenotypeAnnotation> filteredAnnotations = filterService.filterAnnotations(annotations, pagination.getFieldFilterValueMap());
            filterService.getSortedAndPaginatedAnnotations(pagination, filteredAnnotations, new PhenotypeAnnotationSorting());
            result.setTotal(filteredAnnotations.size());
            result.setResults(filterService.getPaginatedAnnotations(pagination, filteredAnnotations));
        }
        result.calculateRequestDuration(startDate);
        return result;
    }

    public JsonResultResponse<DiseaseAnnotation> getDisease(String alleleID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();

        List<DiseaseAnnotation> annotations = alleleCacheRepo.getDisease(alleleID);

        JsonResultResponse<DiseaseAnnotation> result = new JsonResultResponse<>();

        FilterService<DiseaseAnnotation> filterService = new FilterService<>(new DiseaseAnnotationFiltering());
        if (CollectionUtils.isNotEmpty(annotations)) {
            List<DiseaseAnnotation> filteredAnnotations = filterService.filterAnnotations(annotations, pagination.getFieldFilterValueMap());
            filterService.getSortedAndPaginatedAnnotations(pagination, filteredAnnotations, new DiseaseAnnotationSorting());
            result.setTotal(filteredAnnotations.size());
            result.setResults(filterService.getPaginatedAnnotations(pagination, filteredAnnotations));
            ColumnFieldMapping<DiseaseAnnotation> mapping = new AlleleDiseaseColumnFieldMapping();
            result.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(annotations,
                    mapping.getSingleValuedFieldColumns(Table.ALLELE_DISEASE), mapping));
        }
        result.calculateRequestDuration(startDate);
        return result;
    }

    public JsonResultResponse<Allele> getTransgenicAlleles(String geneID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        List<Allele> alleles = alleleRepo.getTransgenicAlleles(geneID);
        JsonResultResponse<Allele> result = new JsonResultResponse<>();

        // filter
        if (CollectionUtils.isNotEmpty(alleles)) {
            FilterService<Allele> filterService = new FilterService<>(new AlleleFiltering());
            List<Allele> filteredAlleles = filterService.filterAnnotations(alleles, pagination.getFieldFilterValueMap());
            result.setTotal(filteredAlleles.size());
            result.setResults(filterService.getPaginatedAnnotations(pagination, filteredAlleles));
            ColumnFieldMapping<Allele> mapping = new TransgenicAlleleColumnFieldMapping();
            result.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(alleles,
                    mapping.getSingleValuedFieldColumns(Table.TRANSGENIC_ALLELE), mapping));
        }
        // sort
        result.calculateRequestDuration(startDate);
        return result;
    }
}
