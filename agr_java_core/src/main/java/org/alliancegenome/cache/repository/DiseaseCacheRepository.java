package org.alliancegenome.cache.repository;

import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.api.service.FilterService;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCacheManager;
import org.alliancegenome.cache.manager.DiseaseAllianceCacheManager;
import org.alliancegenome.cache.manager.ExpressionAllianceCacheManager;
import org.alliancegenome.core.service.DiseaseAnnotationFiltering;
import org.alliancegenome.core.service.DiseaseAnnotationSorting;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.PublicationEvidenceCodeJoin;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;

@Log4j2
public class DiseaseCacheRepository {


    // Map<gene ID, List<DiseaseAnnotation>> including annotations to child terms
    private static Map<String, List<DiseaseAnnotation>> diseaseAnnotationExperimentGeneMap = new HashMap<>();
    // Map<gene ID, List<DiseaseAnnotation>> including annotations to child terms
    private static Map<String, List<DiseaseAnnotation>> diseaseAnnotationOrthologGeneMap = new HashMap<>();

    public List<DiseaseAnnotation> getDiseaseAnnotationList(String diseaseID) {
        DiseaseAllianceCacheManager manager = new DiseaseAllianceCacheManager();
        return manager.getDiseaseAnnotations(diseaseID, View.DiseaseCacher.class);
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
        List<DiseaseAnnotation> fullDiseaseAnnotationList = new ArrayList<>();

        DiseaseAllianceCacheManager manager = new DiseaseAllianceCacheManager();
        // filter by gene
        geneIDs.forEach(geneID -> {
                    List<DiseaseAnnotation> annotations = manager.getDiseaseAnnotations(geneID, View.DiseaseCacher.class);
                    if (annotations != null)
                        fullDiseaseAnnotationList.addAll(annotations);
                    else
                        log.info("no disease annotation found for gene: " + geneID);
                }
        );
        // filter by slim ID
        List<DiseaseAnnotation> slimDiseaseAnnotationList = new ArrayList<>();
        if (StringUtils.isNotEmpty(diseaseSlimID)) {
            if (!diseaseSlimID.equals(DiseaseRibbonSummary.DOID_OTHER)) {
                slimDiseaseAnnotationList = fullDiseaseAnnotationList.stream()
                        .filter(diseaseAnnotation -> diseaseAnnotation.getParentIDs().contains(diseaseSlimID))
                        .collect(toList());
            } else {
                // loop over all Other root terms and check
                slimDiseaseAnnotationList.addAll(DiseaseService.getAllOtherDiseaseTerms().stream()
                        .map(termID -> fullDiseaseAnnotationList.stream()
                                .filter(diseaseAnnotation -> diseaseAnnotation.getParentIDs().contains(diseaseSlimID))
                                .collect(toList()))
                        .flatMap(Collection::stream)
                        .collect(toList()));
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

    public List<ECOTerm> getEcoTerm(List<PublicationEvidenceCodeJoin> joins) {
        if (joins == null)
            return null;
        BasicCacheManager<String> manager = new BasicCacheManager<>();
        List<ECOTerm> list = new ArrayList<>();
        CollectionType javaType = BasicCacheManager.mapper.getTypeFactory()
                .constructCollectionType(List.class, ECOTerm.class);

        joins.forEach(join -> {
            String json = manager.getCache(join.getPrimaryKey(), CacheAlliance.ECO_MAP);
            if (json == null)
                return;
            try {
                list.addAll(BasicCacheManager.mapper.readValue(json, javaType));
            } catch (IOException e) {
                log.error("Error during deserialization ", e);
                throw new RuntimeException(e);
            }
        });
        return list;
    }

    public List<String> getChildren(String id) {
        BasicCacheManager<String> manager = new BasicCacheManager<>();
        List<String> list = new ArrayList<>();
        CollectionType javaType = BasicCacheManager.mapper.getTypeFactory()
                .constructCollectionType(List.class, String.class);

        String json = manager.getCache(id, CacheAlliance.CLOSURE_MAP);
        try {
            list.addAll(BasicCacheManager.mapper.readValue(json, javaType));
        } catch (IOException e) {
            log.error("Error during deserialization ", e);
            throw new RuntimeException(e);
        }
        return list;
    }

    private DiseaseAllianceCacheManager manager = new DiseaseAllianceCacheManager();

    public boolean hasDiseaseAnnotations(String geneID) {
        return CollectionUtils.isNotEmpty(manager.getDiseaseAnnotations(geneID, View.DiseaseAnnotation.class));
    }
}
