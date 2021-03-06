package org.alliancegenome.cache.repository;

import static java.util.stream.Collectors.toList;

import java.util.*;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.service.FilterService;
import org.alliancegenome.cache.*;
import org.alliancegenome.cache.repository.helper.*;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.*;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.apache.commons.collections.CollectionUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RequestScoped
public class PhenotypeCacheRepository {

    @Inject
    private CacheService cacheService;

    public PaginationResult<PhenotypeAnnotation> getPhenotypeAnnotationList(String geneID, Pagination pagination) {

        List<PhenotypeAnnotation> fullPhenotypeAnnotationList = getPhenotypeAnnotationList(geneID);

        // remove GENE annotations from PAE list
        //filtering
        PaginationResult<PhenotypeAnnotation> result = new PaginationResult<>();
        FilterService<PhenotypeAnnotation> filterService = new FilterService<>(new PhenotypeAnnotationFiltering());
        if (CollectionUtils.isNotEmpty(fullPhenotypeAnnotationList)) {
            List<PhenotypeAnnotation> filteredAnnotations = filterService.filterAnnotations(fullPhenotypeAnnotationList, pagination.getFieldFilterValueMap());
            filterService.getSortedAndPaginatedAnnotations(pagination, filteredAnnotations, new PhenotypeAnnotationSorting());
            result.setTotalNumber(filteredAnnotations.size());
            result.setResult(filterService.getPaginatedAnnotations(pagination, filteredAnnotations));
        }
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

    public List<PhenotypeAnnotation> getPhenotypeAnnotationList(String geneID) {

        List<PhenotypeAnnotation> cache = cacheService.getCacheEntries(geneID, CacheAlliance.GENE_PHENOTYPE);
        Optional<List<PhenotypeAnnotation>> optional = Optional.ofNullable(cache);
        return optional.orElse(new ArrayList<>());
    }

    public List<PrimaryAnnotatedEntity> getPhenotypeAnnotationPureModeList(String geneID) {
        return cacheService.getCacheEntries(geneID, CacheAlliance.GENE_PURE_AGM_PHENOTYPE);
    }
}
