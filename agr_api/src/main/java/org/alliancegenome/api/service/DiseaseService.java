package org.alliancegenome.api.service;

import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.es.index.site.dao.DiseaseDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchResponse;
import org.alliancegenome.es.util.SearchHitIterator;

@RequestScoped
public class DiseaseService {

    private static DiseaseDAO diseaseDAO = new DiseaseDAO();

    public Map<String, Object> getById(String id) {
        return diseaseDAO.getById(id);
    }

    public SearchResponse getDiseaseAnnotations(String id, Pagination pagination) {
        return diseaseDAO.getDiseaseAnnotations(id, pagination);
    }


    public SearchHitIterator getDiseaseAnnotationsDownload(String id, Pagination pagination) {
        return diseaseDAO.getDiseaseAnnotationsDownload(id, pagination);
    }
}
