package org.alliancegenome.api.service;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.DiseaseSummary;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.DiseaseCacheRepository;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.enterprise.context.RequestScoped;
import java.time.LocalDateTime;

@RequestScoped
public class DiseaseService {

    private Log log = LogFactory.getLog(getClass());
    private static DiseaseRepository diseaseRepository = new DiseaseRepository();
    private static DiseaseCacheRepository diseaseCacheRepository = new DiseaseCacheRepository();

    public DiseaseService() {

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

    private PaginationResult<DiseaseAnnotation> getDiseaseAnnotationList(String geneID, Pagination pagination, boolean empiricalDisease) {
        return diseaseCacheRepository.getDiseaseAnnotationList(geneID, pagination, empiricalDisease);
    }


    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotations(String geneID, Pagination pagination, boolean empiricalDisease) {
        LocalDateTime startDate = LocalDateTime.now();
        PaginationResult<DiseaseAnnotation> result = getDiseaseAnnotationList(geneID, pagination, empiricalDisease);
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
}

