package org.alliancegenome.cache.repository;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.api.service.FilterService;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;
import org.alliancegenome.core.service.DiseaseAnnotationFiltering;
import org.alliancegenome.core.service.DiseaseAnnotationSorting;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.PublicationJoin;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Log4j2
public class DiseaseCacheRepository {

    private BasicCachingManager<DiseaseAnnotation> manager = new BasicCachingManager<>(DiseaseAnnotation.class);

    // Map<gene ID, List<DiseaseAnnotation>> including annotations to child terms
    private static Map<String, List<DiseaseAnnotation>> diseaseAnnotationExperimentGeneMap = new HashMap<>();
    // Map<gene ID, List<DiseaseAnnotation>> including annotations to child terms
    private static Map<String, List<DiseaseAnnotation>> diseaseAnnotationOrthologGeneMap = new HashMap<>();

    public List<DiseaseAnnotation> getDiseaseAnnotationList(String diseaseID) {
        BasicCachingManager<DiseaseAnnotation> manager = new BasicCachingManager<>(DiseaseAnnotation.class);
        return manager.getCache(diseaseID, CacheAlliance.DISEASE_ANNOTATION);
    }

    public List<DiseaseAnnotation> getDiseaseAlleleAnnotationList(String diseaseID) {
        return manager.getCache(diseaseID, CacheAlliance.DISEASE_ALLELE_ANNOTATION);
    }

    public List<PrimaryAnnotatedEntity> getPrimaryAnnotatedEntitList(String geneID) {
        BasicCachingManager<PrimaryAnnotatedEntity> manager = new BasicCachingManager<>(PrimaryAnnotatedEntity.class);
        return manager.getCache(geneID, CacheAlliance.GENE_MODEL);
    }

    public PaginationResult<DiseaseAnnotation> getDiseaseAnnotationList(String diseaseID, Pagination pagination) {

        List<DiseaseAnnotation> fullDiseaseAnnotationList = getDiseaseAnnotationList(diseaseID);
        PaginationResult<DiseaseAnnotation> result = new PaginationResult<>();
        if (fullDiseaseAnnotationList == null) {
            return result;
        }

        //filtering
        result = getDiseaseAnnotationPaginationResult(pagination, fullDiseaseAnnotationList);
        return result;
    }

    public PaginationResult<DiseaseAnnotation> getRibbonDiseaseAnnotations(List<String> geneIDs, String diseaseSlimID, Pagination pagination) {

        if (geneIDs == null)
            return null;
        Set<DiseaseAnnotation> allDiseaseAnnotationList = new HashSet<>();

        // filter by gene
        geneIDs.forEach(geneID -> {
                    List<DiseaseAnnotation> annotations = manager.getCache(geneID, CacheAlliance.DISEASE_ANNOTATION);
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

    public PaginationResult<DiseaseAnnotation> getDiseaseAnnotationList(String geneID, Pagination pagination, boolean empiricalDisease) {

        List<DiseaseAnnotation> diseaseAnnotationList;
        if (empiricalDisease)
            diseaseAnnotationList = diseaseAnnotationExperimentGeneMap.get(geneID);
        else
            diseaseAnnotationList = diseaseAnnotationOrthologGeneMap.get(geneID);
        if (diseaseAnnotationList == null)
            return null;

        //filtering
        PaginationResult<DiseaseAnnotation> result = getDiseaseAnnotationPaginationResult(pagination, diseaseAnnotationList);
        return result;
    }

    public List<ECOTerm> getEcoTerms(List<PublicationJoin> joins) {
        if (joins == null)
            return null;
        BasicCachingManager<ECOTerm> manager = new BasicCachingManager<>();
        List<ECOTerm> list = new ArrayList<>();
        joins.forEach(join -> {
            list.addAll(manager.getCache(join.getPrimaryKey(), CacheAlliance.ECO_MAP));
        });
        return list;
    }

    public List<ECOTerm> getEcoTerm(PublicationJoin join) {
        if (join == null)
            return null;
        BasicCachingManager<ECOTerm> manager = new BasicCachingManager<>();
        return manager.getCache(join.getPrimaryKey(), CacheAlliance.ECO_MAP);
    }

    public List<ECOTerm> getEcoTermsFromCache(List<PublicationJoin> joins) {
        if (joins == null)
            return null;

        return joins.stream()
                .map(join -> getEcoTerm(join))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public void populatePublicationJoinsFromCache(List<PublicationJoin> joins) {
        if (joins == null)
            return;

        joins.forEach(publicationJoin -> {
            List<ECOTerm> cacheValue = getEcoTerm(publicationJoin);
            if (cacheValue != null) {
                publicationJoin.setEcoCode(cacheValue);
            }
        });
    }

    public List<String> getChildren(String id) {
        BasicCachingManager<String> manager = new BasicCachingManager<>();
        return manager.getCache(id, CacheAlliance.CLOSURE_MAP);
    }

    public boolean hasDiseaseAnnotations(String geneID) {
        return CollectionUtils.isNotEmpty(manager.getCache(geneID, CacheAlliance.DISEASE_ANNOTATION));
    }

    public List<PrimaryAnnotatedEntity> getDiseaseAnnotationPureModeList(String geneID) {
        BasicCachingManager<PrimaryAnnotatedEntity> manager = new BasicCachingManager<>(PrimaryAnnotatedEntity.class);
        return manager.getCache(geneID, CacheAlliance.GENE_PURE_AGM_DISEASE);
    }
}
