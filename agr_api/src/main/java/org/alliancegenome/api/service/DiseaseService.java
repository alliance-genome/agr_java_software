package org.alliancegenome.api.service;

import org.alliancegenome.api.entity.DiseaseEntitySubgroupSlim;
import org.alliancegenome.api.entity.DiseaseRibbonEntity;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.cache.repository.DiseaseCacheRepository;
import org.alliancegenome.core.service.*;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.DiseaseSummary;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.SimpleTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.BaseFilter;

import javax.enterprise.context.RequestScoped;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequestScoped
public class DiseaseService {

    private static DiseaseRepository diseaseRepository = new DiseaseRepository();
    private static GeneRepository geneRepository = new GeneRepository();
    private static DiseaseCacheRepository diseaseCacheRepository = new DiseaseCacheRepository();

    public DiseaseService() {

    }

    public static List<String> getDiseaseParents(String diseaseSlimID) {
        if (!diseaseSlimID.equals(DiseaseRibbonSummary.DOID_OTHER)) {
            ArrayList<String> strings = new ArrayList<>();
            strings.add(diseaseSlimID);
            return strings;
        }
        return new ArrayList<>(Arrays.asList("DOID:0080015", "DOID:0014667", "DOID:150", "DOID:225"));
    }

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
        }
        return response;
    }

    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsWithAlleles(String diseaseID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        List<DiseaseAnnotation> fullDiseaseAnnotationList = diseaseCacheRepository.getDiseaseAnnotationList(diseaseID);
        JsonResultResponse<DiseaseAnnotation> result = new JsonResultResponse<>();
        if (fullDiseaseAnnotationList == null) {
            result.calculateRequestDuration(startDate);
            return result;
        }

        List<DiseaseAnnotation> alleleDiseaseAnnotations = fullDiseaseAnnotationList.stream()
                .filter(annotation -> annotation.getFeature() != null)
                .collect(Collectors.toList());
        //filtering
        List<DiseaseAnnotation> filteredDiseaseAnnotationList = filterDiseaseAnnotations(alleleDiseaseAnnotations, pagination.getFieldFilterValueMap());
        result.setTotal(alleleDiseaseAnnotations.size());
        result.setResults(getSortedAndPaginatedDiseaseAnnotations(pagination, filteredDiseaseAnnotationList));
        return result;
    }

    private List<DiseaseAnnotation> filterDiseaseAnnotations(List<DiseaseAnnotation> diseaseAnnotationList, BaseFilter fieldFilterValueMap) {
        if (diseaseAnnotationList == null)
            return null;
        if (fieldFilterValueMap == null)
            return diseaseAnnotationList;
        return diseaseAnnotationList.stream()
                .filter(annotation -> containsFilterValue(annotation, fieldFilterValueMap))
                .collect(Collectors.toList());
    }

    private boolean containsFilterValue(DiseaseAnnotation annotation, BaseFilter fieldFilterValueMap) {
        // remove entries with null values.
        fieldFilterValueMap.values().removeIf(Objects::isNull);

        Set<Boolean> filterResults = fieldFilterValueMap.entrySet().stream()
                .map((entry) -> {
                    FilterFunction<DiseaseAnnotation, String> filterFunction = DiseaseAnnotationFiltering.filterFieldMap.get(entry.getKey());
                    if (filterFunction == null)
                        return null;
                    return filterFunction.containsFilterValue(annotation, entry.getValue());
                })
                .collect(Collectors.toSet());

        return !filterResults.contains(false);
    }

    private List<DiseaseAnnotation> getSortedAndPaginatedDiseaseAnnotations(Pagination pagination, List<DiseaseAnnotation> fullDiseaseAnnotationList) {
        // sorting
        SortingField sortingField = null;
        String sortBy = pagination.getSortBy();
        if (sortBy != null && !sortBy.isEmpty())
            sortingField = SortingField.getSortingField(sortBy.toUpperCase());

        DiseaseAnnotationSorting sorting = new DiseaseAnnotationSorting();
        fullDiseaseAnnotationList.sort(sorting.getComparator(sortingField, pagination.getAsc()));

        // paginating
        return fullDiseaseAnnotationList.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(Collectors.toList());
    }

    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotations(String geneID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        PaginationResult<DiseaseAnnotation> result = diseaseCacheRepository.getDiseaseAnnotationList(geneID, pagination);
        JsonResultResponse<DiseaseAnnotation> response = new JsonResultResponse<>();
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
        if (result != null) {
            response.setResults(result.getResult());
            response.setTotal(result.getTotalNumber());
        }
        response.calculateRequestDuration(startDate);
        return response;
    }

    public DiseaseSummary getDiseaseSummary(String id, DiseaseSummary.Type type) {
        return diseaseRepository.getDiseaseSummary(id, type);
    }

    public DiseaseRibbonSummary getDiseaseRibbonSummary(List<String> geneIDs) {
        DiseaseRibbonService diseaseRibbonService = new DiseaseRibbonService();
        DiseaseRibbonSummary summary = diseaseRibbonService.getDiseaseRibbonSectionInfo();
        Pagination pagination = new Pagination();
        pagination.setLimitToAll();
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
                .collect(Collectors.toList());
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

    private DiseaseRibbonService diseaseRibbonService = new DiseaseRibbonService();

    private Map<String, List<DiseaseAnnotation>> getDiseaseAnnotationHistogram(PaginationResult<DiseaseAnnotation> paginationResult) {
        Map<String, List<DiseaseAnnotation>> histogram = new HashMap<>();
        if (paginationResult.getResult() == null)
            return histogram;
        paginationResult.getResult().forEach(annotation -> {
            Set<String> slimIds = diseaseRibbonService.getSlimId(annotation.getDisease().getPrimaryKey());
            slimIds.forEach(slimId -> {
                List<DiseaseAnnotation> list = histogram.get(slimId);
                if (list == null)
                    list = new ArrayList<>();
                list.add(annotation);
                histogram.put(slimId, list);
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
        }
        return response;
    }
}

