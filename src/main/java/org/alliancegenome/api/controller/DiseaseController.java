package org.alliancegenome.api.controller;

import org.alliancegenome.api.model.SearchResult;
import org.alliancegenome.api.rest.interfaces.DiseaseRESTInterface;
import org.alliancegenome.api.service.DiseaseService;
import org.jboss.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Map;

@RequestScoped
public class DiseaseController implements DiseaseRESTInterface {

    private Logger log = Logger.getLogger(getClass());

    @Inject
    private DiseaseService diseaseService;

    @Override
    public Map<String, Object> getDisease(String id) {
        return diseaseService.getById(id);
    }

    @Override
    public SearchResult getDiseaseAnnotations(String id, int limit, int offset) {
        return diseaseService.getDiseaseAnnotations(id, offset, limit);
    }

}
