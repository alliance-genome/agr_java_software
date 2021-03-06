package org.alliancegenome.api.service;

import static java.util.stream.Collectors.*;
import static org.alliancegenome.neo4j.entity.DiseaseAnnotation.NOT_ASSOCIATION_TYPE;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.entity.*;
import org.alliancegenome.cache.repository.*;
import org.alliancegenome.cache.repository.helper.*;
import org.alliancegenome.es.model.query.*;
import org.alliancegenome.neo4j.entity.*;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.*;
import org.apache.commons.collections.CollectionUtils;

@RequestScoped
public class DiseaseService {

    private static DiseaseRepository diseaseRepository = new DiseaseRepository();
    private static GeneRepository geneRepository = new GeneRepository();

    @Inject
    private DiseaseCacheRepository diseaseCacheRepository;

    @Inject
    private PhenotypeCacheRepository phenotypeCacheRepository;

    private DiseaseRibbonService diseaseRibbonService = new DiseaseRibbonService(diseaseRepository);

    public DOTerm getById(String id) {
        return diseaseRepository.getDiseaseTerm(id);
    }

    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsByDisease(String diseaseID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        PaginationResult<DiseaseAnnotation> paginationResult = diseaseCacheRepository.getDiseaseAnnotationList(diseaseID, pagination);
        JsonResultResponse<DiseaseAnnotation> response = new JsonResultResponse<>();
        response.calculateRequestDuration(startDate);
        if (paginationResult != null) {
            response.setResults(paginationResult.getResult());
            response.setTotal(paginationResult.getTotalNumber());
            response.addDistinctFieldValueSupplementalData(paginationResult.getDistinctFieldValueMap());
        }
        return response;
    }

    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsWithAlleles(String diseaseID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        List<DiseaseAnnotation> fullDiseaseAnnotationList = diseaseCacheRepository.getDiseaseAlleleAnnotationList(diseaseID);
        JsonResultResponse<DiseaseAnnotation> result = new JsonResultResponse<>();
        if (fullDiseaseAnnotationList == null) {
            result.calculateRequestDuration(startDate);
            return result;
        }

        List<DiseaseAnnotation> alleleDiseaseAnnotations = fullDiseaseAnnotationList.stream()
                .filter(annotation -> annotation.getFeature() != null)
                .collect(toList());
        //filtering
        FilterService<DiseaseAnnotation> filterService = new FilterService<>(new DiseaseAnnotationFiltering());
        List<DiseaseAnnotation> filteredDiseaseAnnotationList = filterService.filterAnnotations(alleleDiseaseAnnotations, pagination.getFieldFilterValueMap());
        result.setTotal(filteredDiseaseAnnotationList.size());
        result.setResults(filterService.getSortedAndPaginatedAnnotations(pagination, filteredDiseaseAnnotationList, new DiseaseAnnotationSorting()));

        ColumnFieldMapping<DiseaseAnnotation> mapping = new DiseaseColumnFieldMapping();
        result.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(alleleDiseaseAnnotations,
                mapping.getSingleValuedFieldColumns(Table.ALLELE), mapping));

        return result;
    }

    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsWithGenes(String geneID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        List<DiseaseAnnotation> fullDiseaseAnnotationList = diseaseCacheRepository.getDiseaseAnnotationList(geneID);
        JsonResultResponse<DiseaseAnnotation> result = new JsonResultResponse<>();
        if (fullDiseaseAnnotationList == null) {
            result.calculateRequestDuration(startDate);
            return result;
        }

        // need to group annotations by gene / association type
        Map<Gene, Map<String, List<DiseaseAnnotation>>> groupedByGeneList = fullDiseaseAnnotationList.stream()
                .filter(diseaseAnnotation -> diseaseAnnotation.getGene() != null)
                .collect(Collectors.groupingBy(DiseaseAnnotation::getGene,
                        Collectors.groupingBy(DiseaseAnnotation::getAssociationType)));

        List<DiseaseAnnotation> geneDiseaseAnnotations = new ArrayList<>();
        groupedByGeneList.forEach((gene, typeMap) -> {
            typeMap.forEach((type, diseaseAnnotations) -> {
                Map<String, List<DiseaseAnnotation>> groupedDAs = diseaseAnnotations.stream()
                        .collect(groupingBy(o -> o.getDisease().getPrimaryKey()));
                groupedDAs.forEach((s, annotations) -> {
                    DiseaseAnnotation firstAnnotation = annotations.get(0);
                    annotations.forEach(annotation -> {
                        firstAnnotation.addAllPrimaryAnnotatedEntities(annotation.getPrimaryAnnotatedEntities());
                        firstAnnotation.addOrthologousGenes(annotation.getOrthologyGenes());
                    });
                    geneDiseaseAnnotations.add(firstAnnotation);
                });
            });
        });
        //filtering
        FilterService<DiseaseAnnotation> filterService = new FilterService<>(new DiseaseAnnotationFiltering());
        List<DiseaseAnnotation> filteredDiseaseAnnotationList = filterService.filterAnnotations(geneDiseaseAnnotations, pagination.getFieldFilterValueMap());
        result.setTotal(filteredDiseaseAnnotationList.size());
        result.setResults(filterService.getSortedAndPaginatedAnnotations(pagination, filteredDiseaseAnnotationList, new DiseaseAnnotationSorting()));

        ColumnFieldMapping<DiseaseAnnotation> mapping = new DiseaseColumnFieldMapping();
        result.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(geneDiseaseAnnotations,
                mapping.getSingleValuedFieldColumns(Table.ASSOCIATED_GENE), mapping));

        return result;
    }

    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsWithAGM(String diseaseID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        List<DiseaseAnnotation> fullDiseaseAnnotationList = diseaseCacheRepository.getDiseaseModelAnnotations(diseaseID);
        JsonResultResponse<DiseaseAnnotation> result = new JsonResultResponse<>();
        if (fullDiseaseAnnotationList == null) {
            result.calculateRequestDuration(startDate);
            result.addDistinctFieldValueSupplementalData(new HashMap());
            return result;
        }

        // select list of annotations to model entities
        List<DiseaseAnnotation> modelDiseaseAnnotations = fullDiseaseAnnotationList.stream()
                .filter(diseaseAnnotation -> diseaseAnnotation.getModel() != null)
                .collect(Collectors.toList());

        //filtering
        FilterService<DiseaseAnnotation> filterService = new FilterService<>(new ModelAnnotationFiltering());
        List<DiseaseAnnotation> filteredDiseaseAnnotationList = filterService.filterAnnotations(modelDiseaseAnnotations, pagination.getFieldFilterValueMap());
        result.setTotal(filteredDiseaseAnnotationList.size());
        result.setResults(filterService.getSortedAndPaginatedAnnotations(pagination, filteredDiseaseAnnotationList, new ModelAnnotationsSorting()));
        result.calculateRequestDuration(startDate);

        ColumnFieldMapping<DiseaseAnnotation> mapping = new DiseaseColumnFieldMapping();
        result.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(modelDiseaseAnnotations,
                mapping.getSingleValuedFieldColumns(Table.MODEL), mapping));

        return result;
    }

    public JsonResultResponse<PrimaryAnnotatedEntity> getDiseaseAnnotationsWithGeneAndAGM(String geneID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        JsonResultResponse<PrimaryAnnotatedEntity> result = new JsonResultResponse<>();

        List<PrimaryAnnotatedEntity> pureModelList = phenotypeCacheRepository.getPhenotypeAnnotationPureModeList(geneID);

        List<PrimaryAnnotatedEntity> pureDiseaseModelList = diseaseCacheRepository.getDiseaseAnnotationPureModeList(geneID);

        List<PrimaryAnnotatedEntity> fullModelList = diseaseCacheRepository.getPrimaryAnnotatedEntitList(geneID);


        if (CollectionUtils.isEmpty(pureModelList) && CollectionUtils.isEmpty(pureDiseaseModelList) && CollectionUtils.isEmpty(fullModelList)) {
            return result;
        }

        List<String> mergedEntities = new ArrayList<>();
        // add AGMs to the ones created in the disease cycle
        if (CollectionUtils.isNotEmpty(pureDiseaseModelList) && CollectionUtils.isNotEmpty(pureModelList)) {
            for (PrimaryAnnotatedEntity entity : pureDiseaseModelList) {
                pureModelList.stream()
                        .filter(entity1 -> entity1.getId().equals(entity.getId()))
                        .forEach(entity1 -> {
                            entity.addPhenotypes(entity1.getPhenotypes());
                            mergedEntities.add(entity.getId());
                        });
            }
            // remove the merged ones
            pureModelList = pureModelList.stream()
                    .filter(entity -> !mergedEntities.contains(entity.getId()))
                    .collect(toList());
            pureDiseaseModelList.addAll(pureModelList);
        }

        if (CollectionUtils.isEmpty(pureDiseaseModelList) && CollectionUtils.isNotEmpty(pureModelList)) {
            pureDiseaseModelList = pureModelList;
        }
        if (CollectionUtils.isEmpty(pureDiseaseModelList) && CollectionUtils.isEmpty(pureModelList)) {
            pureDiseaseModelList = new ArrayList<>();
        }

        List<String> geneIDs = pureDiseaseModelList.stream()
                .map(PrimaryAnnotatedEntity::getId)
                .collect(toList());

        // remove the AGMs (from the general model list) that are already accounted for by disease or phenotype relationship
        // leaving only those AGMs that have no one of that kind
        if (CollectionUtils.isNotEmpty(fullModelList)) {
            geneIDs.forEach(geneId -> {
                fullModelList.removeIf(entity -> entity.getId().equals(geneId));
            });
            pureDiseaseModelList.addAll(fullModelList);
        }

        //filtering
        FilterService<PrimaryAnnotatedEntity> filterService = new FilterService<>(new PrimaryAnnotatedEntityFiltering());
        List<PrimaryAnnotatedEntity> filteredDiseaseAnnotationList = filterService.filterAnnotations(pureDiseaseModelList, pagination.getFieldFilterValueMap());
        result.setTotal(filteredDiseaseAnnotationList.size());
        result.setResults(filterService.getSortedAndPaginatedAnnotations(pagination, filteredDiseaseAnnotationList, new PrimaryAnnotatedEntitySorting()));
        result.calculateRequestDuration(startDate);
        return result;
    }

    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotations(String geneID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        JsonResultResponse<DiseaseAnnotation> response = getDiseaseAnnotationsWithGenes(geneID, pagination);
        String note = "";
        if (!SortingField.isValidSortingFieldValue(pagination.getSortBy())) {
            note += "Invalid sorting name provided: " + pagination.getSortBy();
            note += ". Sorting is ignored! ";
            note += "Allowed values are (case insensitive): " + SortingField.getAllValues();
        }
        if (pagination.hasInvalidElements()) {
            note += "Invalid filtering name(s) provided: " + pagination.getInvalidFilterList();
            note += ". Filtering for these elements is ignored! ";
            note += "Allowed values are (case insensitive): " + FieldFilter.getAllValues();
        }
        if (!note.isEmpty())
            response.setNote(note);
        response.calculateRequestDuration(startDate);
        return response;
    }

    public DiseaseSummary getDiseaseSummary(String id, DiseaseSummary.Type type) {
        return diseaseRepository.getDiseaseSummary(id, type);
    }

    public DiseaseRibbonSummary getDiseaseRibbonSummary(List<String> geneIDs, String includeNegation) {
        DiseaseRibbonSummary summary = diseaseRibbonService.getDiseaseRibbonSectionInfo();
        Pagination pagination = new Pagination();
        pagination.setLimitToAll();
        pagination.addFieldFilter(FieldFilter.INCLUDE_NEGATION, includeNegation);
        // loop over all genes provided
        geneIDs.forEach(geneID -> {
            PaginationResult<DiseaseAnnotation> paginationResult = diseaseCacheRepository.getDiseaseAnnotationList(geneID, pagination);
            if (paginationResult == null) {
                paginationResult = new PaginationResult<>();
                paginationResult.setTotalNumber(0);
            }
            // calculate histogram
            Map<String, List<DiseaseAnnotation>> histogram = getDiseaseAnnotationHistogram(paginationResult);

            Gene gene = geneRepository.getShallowGene(geneID);
            if (gene == null)
                return;
            // populate diseaseEntity records
            populateDiseaseRibbonSummary(geneID, summary, histogram, gene);
            summary.addAllAnnotationsCount(geneID, paginationResult.getTotalNumber());
        });
        return summary;
    }

    private void populateDiseaseRibbonSummary(String geneID, DiseaseRibbonSummary summary, Map<String, List<DiseaseAnnotation>> histogram, Gene gene) {
        DiseaseRibbonEntity entity = new DiseaseRibbonEntity();
        entity.setId(geneID);
        entity.setLabel(gene.getSymbol());
        entity.setTaxonID(gene.getTaxonId());
        entity.setTaxonName(gene.getSpecies().getName());
        summary.addDiseaseRibbonEntity(entity);

        Set<String> allTerms = new HashSet<>();
        Set<DiseaseAnnotation> allAnnotations = new HashSet<>();
        List<String> agrDoSlimIDs = diseaseRepository.getAgrDoSlim().stream()
                .map(SimpleTerm::getPrimaryKey)
                .collect(toList());
        // add category term IDs to get the full histogram mapped into the response
        agrDoSlimIDs.addAll(DiseaseRibbonService.slimParentTermIdMap.keySet());
        agrDoSlimIDs.forEach(slimId -> {
            DiseaseEntitySubgroupSlim group = new DiseaseEntitySubgroupSlim();
            int size = 0;
            List<DiseaseAnnotation> diseaseAnnotations = histogram.get(slimId);
            if (diseaseAnnotations != null) {
                allAnnotations.addAll(diseaseAnnotations);
                size = diseaseAnnotations.size();
                Set<String> terms = diseaseAnnotations.stream().map(diseaseAnnotation -> diseaseAnnotation.getDisease().getPrimaryKey())
                        .collect(Collectors.toSet());
                allTerms.addAll(terms);
                group.setNumberOfClasses(terms.size());
            }
            group.setNumberOfAnnotations(size);
            group.setId(slimId);
            if (size > 0)
                entity.addDiseaseSlim(group);
        });
        entity.setNumberOfClasses(allTerms.size());
        entity.setNumberOfAnnotations(allAnnotations.size());
    }

    private Map<String, List<DiseaseAnnotation>> getDiseaseAnnotationHistogram(PaginationResult<DiseaseAnnotation> paginationResult) {
        Map<String, List<DiseaseAnnotation>> histogram = new HashMap<>();
        if (paginationResult.getResult() == null)
            return histogram;
        paginationResult.getResult().forEach(annotation -> {
            Set<String> parentIDs = diseaseRibbonService.getAllParentIDs(annotation.getDisease().getPrimaryKey());
            parentIDs.forEach(parentID -> {
                List<DiseaseAnnotation> list = histogram.get(parentID);
                if (list == null)
                    list = new ArrayList<>();
                list.add(annotation);
                histogram.put(parentID, list);
            });
        });
        return histogram;
    }

    public JsonResultResponse<DiseaseAnnotation> getRibbonDiseaseAnnotations(List<String> geneIDs, String termID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        PaginationResult<DiseaseAnnotation> paginationResult = diseaseCacheRepository.getRibbonDiseaseAnnotations(geneIDs, termID, pagination);
        JsonResultResponse<DiseaseAnnotation> response = new JsonResultResponse<>();
        response.calculateRequestDuration(startDate);
        if (paginationResult != null) {
            response.setResults(paginationResult.getResult());
            response.setTotal(paginationResult.getTotalNumber());
            Map<String, List<String>> distinctFieldValueMap = paginationResult.getDistinctFieldValueMap();
            if(pagination.getFieldFilterValueMap().get(FieldFilter.INCLUDE_NEGATION) == null ||
                    pagination.getFieldFilterValueMap().get(FieldFilter.INCLUDE_NEGATION).equals("false")){
                distinctFieldValueMap.get("associationType").removeIf(o -> o.toLowerCase().contains(NOT_ASSOCIATION_TYPE));
            }
            response.addDistinctFieldValueSupplementalData(distinctFieldValueMap);
        }
        return response;
    }

}

