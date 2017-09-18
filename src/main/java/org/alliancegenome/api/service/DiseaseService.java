package org.alliancegenome.api.service;

import org.alliancegenome.api.dao.DiseaseDAO;
import org.alliancegenome.api.model.SearchResult;
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

    public SearchResult getDiseaseAnnotations(String id, int page, int max) {
        return diseaseDAO.getDiseaseAnnotations(id, page, max);
    }


    public String getDiseaseAnnotationsDownload(String id) {
        return diseaseDAO.getDiseaseAnnotationsDownload(id);
    }
}
