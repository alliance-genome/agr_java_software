package org.alliancegenome.cache.repository;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.service.ColumnFieldMapping;
import org.alliancegenome.api.service.DiseaseColumnFieldMapping;
import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.api.service.FilterService;
import org.alliancegenome.api.service.Table;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.CacheService;
import org.alliancegenome.cache.repository.helper.DiseaseAnnotationFiltering;
import org.alliancegenome.cache.repository.helper.DiseaseAnnotationSorting;
import org.alliancegenome.cache.repository.helper.PaginationResult;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.PublicationJoin;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RequestScoped
public class DiseaseCacheRepository {

    @Inject
    private CacheService cacheService;
    
    private DiseaseCacheRepository() {} // Cannot be instantiated needs to be @Injected

    public List<DiseaseAnnotation> getDiseaseAnnotationList(String diseaseID) {
        return cacheService.getCacheEntries(diseaseID, CacheAlliance.DISEASE_ANNOTATION, DiseaseAnnotation.class);
    }

    public List<DiseaseAnnotation> getDiseaseModelAnnotations(String diseaseID) {
        BasicCachingManager<DiseaseAnnotation> manager = new BasicCachingManager<>(DiseaseAnnotation.class);
        return manager.getCache(diseaseID, CacheAlliance.DISEASE_MODEL_ANNOTATION);
    }

    public List<DiseaseAnnotation> getDiseaseAlleleAnnotationList(String diseaseID) {
        return cacheService.getCacheEntries(diseaseID, CacheAlliance.DISEASE_ALLELE_ANNOTATION, DiseaseAnnotation.class);
    }

    public List<PrimaryAnnotatedEntity> getPrimaryAnnotatedEntitList(String geneID) {
        return cacheService.getCacheEntries(geneID, CacheAlliance.GENE_MODEL, PrimaryAnnotatedEntity.class);
    }

    public PaginationResult<DiseaseAnnotation> getDiseaseAnnotationList(String diseaseID, Pagination pagination) {

        List<DiseaseAnnotation> fullDiseaseAnnotationList = getDiseaseAnnotationList(diseaseID);
        PaginationResult<DiseaseAnnotation> result = new PaginationResult<>();
        if (fullDiseaseAnnotationList == null) {
            return result;
        }

        //filtering
        result = getDiseaseAnnotationPaginationResult(pagination, fullDiseaseAnnotationList);

        FilterService filterService = new FilterService<>(new DiseaseAnnotationFiltering());
        ColumnFieldMapping<DiseaseAnnotation> mapping = new DiseaseColumnFieldMapping();
        result.setDistinctFieldValueMap(filterService.getDistinctFieldValues(fullDiseaseAnnotationList,
                mapping.getSingleValuedFieldColumns(Table.ASSOCIATED_GENE), mapping));
        return result;
    }

    public PaginationResult<DiseaseAnnotation> getRibbonDiseaseAnnotations(List<String> geneIDs, String diseaseSlimID, Pagination pagination) {

        if (geneIDs == null)
            return null;
        Set<DiseaseAnnotation> allDiseaseAnnotationList = new HashSet<>();

        // filter by gene
        geneIDs.forEach(geneID -> {
                    List<DiseaseAnnotation> annotations = cacheService.getCacheEntries(geneID, CacheAlliance.DISEASE_ANNOTATION, DiseaseAnnotation.class);
                    if (annotations != null)
                        allDiseaseAnnotationList.addAll(annotations);
                }
        );
        List<DiseaseAnnotation> fullDiseaseAnnotationList = new ArrayList<>(allDiseaseAnnotationList);
        // filter by slim ID
        List<DiseaseAnnotation> slimDiseaseAnnotationList;
        if (StringUtils.isNotEmpty(diseaseSlimID)) {
            if (!diseaseSlimID.equals(DiseaseRibbonSummary.DOID_OTHER)) {
                slimDiseaseAnnotationList = fullDiseaseAnnotationList.stream()
                        .filter(diseaseAnnotation -> diseaseAnnotation.getParentIDs().contains(diseaseSlimID))
                        .collect(toList());
            } else {
                // loop over all Other root terms and check
                slimDiseaseAnnotationList = DiseaseService.getAllOtherDiseaseTerms().stream()
                        .map(termID -> fullDiseaseAnnotationList.stream()
                                .filter(diseaseAnnotation -> diseaseAnnotation.getParentIDs().contains(diseaseSlimID))
                                .collect(toList()))
                        .flatMap(Collection::stream).distinct().collect(Collectors.toList());
            }

        } else {
            slimDiseaseAnnotationList = fullDiseaseAnnotationList;
        }

        //filtering
        PaginationResult<DiseaseAnnotation> result = getDiseaseAnnotationPaginationResult(pagination, slimDiseaseAnnotationList);
        FilterService<DiseaseAnnotation> filterService = new FilterService<>(new DiseaseAnnotationFiltering());
        ColumnFieldMapping<DiseaseAnnotation> mapping = new DiseaseColumnFieldMapping();
        result.setDistinctFieldValueMap(filterService.getDistinctFieldValues(slimDiseaseAnnotationList,
                mapping.getSingleValuedFieldColumns(Table.DISEASE), mapping));

        return result;
    }

    private PaginationResult<DiseaseAnnotation> getDiseaseAnnotationPaginationResult(Pagination pagination, List<DiseaseAnnotation> slimDiseaseAnnotationList) {
        FilterService<DiseaseAnnotation> filterService = new FilterService<>(new DiseaseAnnotationFiltering());
        List<DiseaseAnnotation> filteredDiseaseAnnotationList = filterService.filterAnnotations(slimDiseaseAnnotationList, pagination.getFieldFilterValueMap());

        PaginationResult<DiseaseAnnotation> result = new PaginationResult<>();
        result.setTotalNumber(filteredDiseaseAnnotationList.size());
        result.setResult(filterService.getSortedAndPaginatedAnnotations(pagination, filteredDiseaseAnnotationList, new DiseaseAnnotationSorting()));
        return result;
    }

    public List<ECOTerm> getEcoTerms(List<PublicationJoin> joins) {
        if (joins == null)
            return null;
        List<ECOTerm> list = new ArrayList<>();
        joins.forEach(join -> {
            list.addAll(cacheService.getCacheEntries(join.getPrimaryKey(), CacheAlliance.ECO_MAP, ECOTerm.class));
        });
        return list;
    }

    public List<String> getChildren(String id) {
        return cacheService.getCacheEntries(id, CacheAlliance.CLOSURE_MAP, String.class);
    }

    public boolean hasDiseaseAnnotations(String geneID) {
        return CollectionUtils.isNotEmpty(cacheService.getCacheEntries(geneID, CacheAlliance.DISEASE_ANNOTATION, DiseaseAnnotation.class));
    }

    public List<PrimaryAnnotatedEntity> getDiseaseAnnotationPureModeList(String geneID) {
        return cacheService.getCacheEntries(geneID, CacheAlliance.DISEASE_MODEL_GENE_ANNOTATION, PrimaryAnnotatedEntity.class);
    }
}
