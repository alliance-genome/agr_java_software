package org.alliancegenome.api.service;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.shared.es.dao.site_index.DiseaseDAO;
import org.alliancegenome.shared.es.model.query.Pagination;
import org.alliancegenome.shared.es.model.search.SearchResult;
import org.alliancegenome.shared.es.util.SearchHitIterator;

@RequestScoped
public class DiseaseService {

    //private Logger log = Logger.getLogger(getClass());

    @Inject
    private DiseaseDAO diseaseDAO;

    public Map<String, Object> getById(String id) {
        return diseaseDAO.getById(id);
    }

    public SearchResult getDiseaseAnnotations(String id, Pagination pagination) {
        return diseaseDAO.getDiseaseAnnotations(id, pagination);
    }


    public SearchHitIterator getDiseaseAnnotationsDownload(String id, Pagination pagination) {
        return diseaseDAO.getDiseaseAnnotationsDownload(id, pagination);
    }
}
