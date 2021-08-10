package org.alliancegenome.api.service;

import org.alliancegenome.api.entity.DiseaseEntitySubgroupSlim;
import org.alliancegenome.api.entity.DiseaseRibbonEntity;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.cache.repository.DiseaseCacheRepository;
import org.alliancegenome.cache.repository.PhenotypeCacheRepository;
import org.alliancegenome.cache.repository.helper.*;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.DiseaseSummary;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.ExperimentalCondition;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.SimpleTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.alliancegenome.neo4j.entity.DiseaseAnnotation.NOT_ASSOCIATION_TYPE;

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

        List<PrimaryAnnotatedEntity> purePhenotypeModelList = phenotypeCacheRepository.getPhenotypeAnnotationPureModeList(geneID);

        List<PrimaryAnnotatedEntity> pureDiseaseModelList = diseaseCacheRepository.getDiseaseAnnotationPureModeList(geneID);

        List<PrimaryAnnotatedEntity> fullModelList = diseaseCacheRepository.getPrimaryAnnotatedEntitList(geneID);


        if (CollectionUtils.isEmpty(purePhenotypeModelList) && CollectionUtils.isEmpty(pureDiseaseModelList) && CollectionUtils.isEmpty(fullModelList)) {
            return result;
        }
        Map<String, Map<String, PrimaryAnnotatedEntity>> groupedEntityMap = new HashMap<>();
        // add AGMs to the ones created in the disease cycle

        if (CollectionUtils.isNotEmpty(pureDiseaseModelList)) {
            // merge disease records
            // by fish and condition
            // assumes only one condition per PAE!
            Map<String, Map<String, List<PrimaryAnnotatedEntity>>> groupedEntityListDisease = getGroupedByMap(pureDiseaseModelList);

            groupedEntityListDisease.forEach((modelID, conditionMap) -> {
                conditionMap.forEach((condition, entities) -> {
                    Map<String, PrimaryAnnotatedEntity> entityMap = groupedEntityMap.computeIfAbsent(modelID,
                            s -> {
                                HashMap<String, PrimaryAnnotatedEntity> map = new HashMap<>();
                                map.put(modelID, null);
                                return map;
                            });
                    PrimaryAnnotatedEntity entity = entityMap.computeIfAbsent(condition, s -> entities.get(0));
                    entities.remove(0);
                    entities.forEach(mergeEntity -> {
                        entity.addPublicationEvidenceCode(mergeEntity.getPublicationEvidenceCodes());
                        entity.addDiseaseModels(mergeEntity.getDiseaseModels());
                    });
                });
            });
        }

        if (CollectionUtils.isNotEmpty(purePhenotypeModelList)) {
            // merge phenotype records
            // by fish and condition
            // assumes only one condition per PAE!
            Map<String, Map<String, List<PrimaryAnnotatedEntity>>> groupedEntityListPhenotype = getGroupedByMap(purePhenotypeModelList);
            groupedEntityListPhenotype.forEach((modelID, conditionMap) -> {
                conditionMap.forEach((condition, entities) -> {
                    Map<String, PrimaryAnnotatedEntity> entityMap = groupedEntityMap.computeIfAbsent(modelID,
                            s -> {
                                HashMap<String, PrimaryAnnotatedEntity> map = new HashMap<>();
                                map.put(modelID, null);
                                return map;
                            });
                    PrimaryAnnotatedEntity entity = entityMap.get(condition);
                    if (entity == null) {
                        entity = entities.get(0);
                        entityMap.put(condition, entity);
                        entities.remove(0);
                    }
                    if (entities.size() > 0) {
                        for (PrimaryAnnotatedEntity mergeEntity : entities) {
                            entity.addPublicationEvidenceCode(mergeEntity.getPublicationEvidenceCodes());
                            entity.addPhenotype(mergeEntity.getPhenotypes().get(0));
                        }
                    }
                });
            });
        }
        if (CollectionUtils.isEmpty(fullModelList)) {
            // merge non-disease and non-phenotype
            // by fish and condition
            // assumes only one condition per PAE!
            Map<String, Map<String, List<PrimaryAnnotatedEntity>>> groupedEntityListNone = getGroupedByMap(fullModelList);
            groupedEntityListNone.forEach((modelID, conditionMap) -> {
                conditionMap.forEach((condition, entities) -> {
                    Map<String, PrimaryAnnotatedEntity> entityMap = groupedEntityMap.computeIfAbsent(modelID,
                            s -> {
                                HashMap<String, PrimaryAnnotatedEntity> map = new HashMap<>();
                                map.put(modelID, null);
                                return map;
                            });
                    PrimaryAnnotatedEntity entity = entityMap.get(condition);
                    if (entity == null) {
                        entity = entities.get(0);
                        entityMap.put(condition, entity);
                        entities.remove(0);
                    }
                    if (entities.size() > 0) {
                        for (PrimaryAnnotatedEntity mergeEntity : entities) {
                            entity.addPublicationEvidenceCode(mergeEntity.getPublicationEvidenceCodes());
                        }
                    }
                });
            });
        }
        List<PrimaryAnnotatedEntity> resultList = groupedEntityMap.values().stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .collect(toList());
        resultList = resultList.stream().filter(Objects::nonNull).collect(toList());

        //filtering
        FilterService<PrimaryAnnotatedEntity> filterService = new FilterService<>(new PrimaryAnnotatedEntityFiltering());
        List<PrimaryAnnotatedEntity> filteredDiseaseAnnotationList = filterService.filterAnnotations(resultList, pagination.getFieldFilterValueMap());
        result.setTotal(filteredDiseaseAnnotationList.size());
        result.setResults(filterService.getSortedAndPaginatedAnnotations(pagination, filteredDiseaseAnnotationList, new PrimaryAnnotatedEntitySorting()));
        result.calculateRequestDuration(startDate);
        return result;
    }

    public Map<String, Map<String, List<PrimaryAnnotatedEntity>>> getGroupedByMap(List<PrimaryAnnotatedEntity> entityList) {
        return entityList.stream()
                .collect(groupingBy(PrimaryAnnotatedEntity::getId,
                        groupingBy(t -> {
                                    if (MapUtils.isNotEmpty(t.getConditions())) {
                                        Map.Entry<String, List<ExperimentalCondition>> conditionType = t.getConditions().entrySet().iterator().next();
                                        StringBuilder key = new StringBuilder(conditionType.getKey() + ":");
                                        conditionType.getValue().stream().sorted(Comparator.comparing(ExperimentalCondition::getConditionStatement)).forEach(experimentalCondition -> {
                                            key.append(experimentalCondition.getConditionStatement()).append(":");
                                        });
                                        return key.toString();
                                    } else
                                        return "No-ExperimentalConditions";
                                }
                        )));
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
            if (pagination.getFieldFilterValueMap().get(FieldFilter.INCLUDE_NEGATION) == null ||
                    pagination.getFieldFilterValueMap().get(FieldFilter.INCLUDE_NEGATION).equals("false")) {
                distinctFieldValueMap.get("associationType").removeIf(o -> o.toLowerCase().contains(NOT_ASSOCIATION_TYPE));
            }
            response.addDistinctFieldValueSupplementalData(distinctFieldValueMap);
        }
        return response;
    }

}

