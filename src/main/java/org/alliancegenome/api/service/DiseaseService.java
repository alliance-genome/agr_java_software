package org.alliancegenome.api.service;

import org.alliancegenome.api.dao.DiseaseDAO;
import org.alliancegenome.api.model.SearchResult;
import org.alliancegenome.api.service.helper.Pagination;
import org.alliancegenome.api.service.helper.SortBy;
import org.jboss.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Map;

@RequestScoped
public class DiseaseService {

    private Logger log = Logger.getLogger(getClass());

    @Inject
    private DiseaseDAO diseaseDAO;

    public Map<String, Object> getById(String id) {
        return diseaseDAO.getById(id);
    }

    public SearchResult getDiseaseAnnotations(String id, Pagination pagination) {
        return diseaseDAO.getDiseaseAnnotations(id, pagination);
    }


    public DiseaseDAO.SearchHitIterator getDiseaseAnnotationsDownload(String id, Pagination pagination) {
        return diseaseDAO.getDiseaseAnnotationsDownload(id, pagination);
    }
}
