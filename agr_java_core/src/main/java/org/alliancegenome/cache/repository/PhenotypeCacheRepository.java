package org.alliancegenome.cache.repository;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.service.FilterService;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.core.service.PhenotypeAnnotationFiltering;
import org.alliancegenome.core.service.PhenotypeAnnotationSorting;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Log4j2
public class PhenotypeCacheRepository {

    public PaginationResult<PhenotypeAnnotation> getPhenotypeAnnotationList(String geneID, Pagination pagination) {

        List<PhenotypeAnnotation> fullPhenotypeAnnotationList = getPhenotypeAnnotationList(geneID);

        // remove GENE annotations from PAE list
        fullPhenotypeAnnotationList.stream()
                .filter(phenotypeAnnotation -> phenotypeAnnotation.getPrimaryAnnotatedEntities() != null)
                .forEach(phenotypeAnnotation -> {
                    phenotypeAnnotation.getPrimaryAnnotatedEntities().removeIf(entity -> entity.getType().equals(GeneticEntity.CrossReferenceType.GENE));
                });

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
        BasicCachingManager<PhenotypeAnnotation> manager = new BasicCachingManager<>(PhenotypeAnnotation.class);
        List<PhenotypeAnnotation> cache = manager.getCache(geneID, CacheAlliance.GENE_PHENOTYPE);
        Optional<List<PhenotypeAnnotation>> optional = Optional.ofNullable(cache);
        return optional.orElse(new ArrayList<>());
    }

    public List<PrimaryAnnotatedEntity> getPhenotypeAnnotationPureModeList(String geneID) {
        BasicCachingManager<PrimaryAnnotatedEntity> manager = new BasicCachingManager<>(PrimaryAnnotatedEntity.class);
        return manager.getCache(geneID, CacheAlliance.GENE_PURE_AGM_PHENOTYPE);
    }
}
