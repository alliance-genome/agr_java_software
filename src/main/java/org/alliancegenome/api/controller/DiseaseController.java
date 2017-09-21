package org.alliancegenome.api.controller;

import org.alliancegenome.api.model.SearchResult;
import org.alliancegenome.api.rest.interfaces.DiseaseRESTInterface;
import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.api.translator.DiseaseAnnotationToTdfTranslator;
import org.jboss.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Map;

@RequestScoped
public class DiseaseController implements DiseaseRESTInterface {

    private Logger log = Logger.getLogger(getClass());

    @Inject
    private DiseaseService diseaseService;
    private DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();


    @Override
    public Map<String, Object> getDisease(String id) {
        return diseaseService.getById(id);
    }

    @Override
    public SearchResult getDiseaseAnnotations(String id, int limit, int offset) {
        return diseaseService.getDiseaseAnnotations(id, offset, limit);
    }

    @Override
    public String getDiseaseAnnotationsDownload(String id) {
        // retrieve all records
        //return diseaseService.getDiseaseAnnotationsDownload(id);
        return translator.getAllRows(diseaseService.getDiseaseAnnotationsDownload(id));
    }
}
