package org.alliancegenome.api.controller;

import org.alliancegenome.api.model.SearchResult;
import org.alliancegenome.api.rest.interfaces.DiseaseRESTInterface;
import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.api.service.helper.Pagination;
import org.alliancegenome.api.service.helper.SortBy;
import org.alliancegenome.api.translator.DiseaseAnnotationToTdfTranslator;
import org.jboss.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Map;

@RequestScoped
public class DiseaseController implements DiseaseRESTInterface {

    private Logger log = Logger.getLogger(getClass());
    @Context  //injected response proxy supporting multiple threads
    private HttpServletResponse response;

    @Inject
    private DiseaseService diseaseService;
    private DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();


    @Override
    public Map<String, Object> getDisease(String id) {
        return diseaseService.getById(id);
    }

    @Override
    public SearchResult getDiseaseAnnotationsSorted(String id, int limit, int page, String sortBy, String asc) {
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        if (pagination.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            try {
                response.flushBuffer();
            } catch (Exception ignored) {
            }
            SearchResult searchResult = new SearchResult();
            searchResult.errorMessages = pagination.getErrorList();
            return searchResult;
        }
        return diseaseService.getDiseaseAnnotations(id, pagination);
    }

    @Override
    public SearchResult getDiseaseAnnotations(String id,
                                              int limit,
                                              int page) {
        return getDiseaseAnnotationsSorted(id, limit, page, null, null);
    }

    @Override
    public Response getDiseaseAnnotationsDownloadFile(String id) {

        Response.ResponseBuilder response = Response.ok(getDiseaseAnnotationsDownload(id));
        response.type(MediaType.TEXT_PLAIN_TYPE);
        response.header("Content-Disposition", "attachment; filename=\"disease-annotations-" + id.replace(":", "-") + ".txt\"");
        return response.build();
    }

    @Override
    public String getDiseaseAnnotationsDownload(String id) {
        Pagination pagination = new Pagination(1, Integer.MAX_VALUE, null, null);
        // retrieve all records
        return translator.getAllRows(diseaseService.getDiseaseAnnotationsDownload(id, pagination));
    }

}
