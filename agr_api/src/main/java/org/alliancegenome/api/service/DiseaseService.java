package org.alliancegenome.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.alliancegenome.es.util.SearchHitIterator;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;

import javax.enterprise.context.RequestScoped;
import java.time.LocalDateTime;
import java.util.List;

@RequestScoped
public class DiseaseService {

    private static DiseaseRepository diseaseRepository = new DiseaseRepository();

    public DOTerm getById(String id) {
        return diseaseRepository.getDiseaseTerm(id);
    }

    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotations(String geneID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
/*
        List<PhenotypeAnnotation> list = getPhenotypeAnnotationList(geneID, pagination);
        JsonResultResponse<PhenotypeAnnotation> response = new JsonResultResponse<>();
        response.calculateRequestDuration(startDate);
        response.setResults(list);
        Long count = phenoRepo.getTotalPhenotypeCount(geneID, pagination);
        response.setTotal((int) (long) count);
        return response;
*/
        return null;
    }


    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsDownload(String id, Pagination pagination) {
        //return diseaseRepository.getDiseaseAnnotationsDownload(id, pagination);
        return null;
    }
}
