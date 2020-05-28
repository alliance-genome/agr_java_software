package org.alliancegenome.api.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.alliancegenome.api.rest.interfaces.OrthologyRESTInterface;
import org.alliancegenome.cache.repository.OrthologyCacheRepository;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.OrthoAlgorithm;
import org.alliancegenome.neo4j.repository.OrthologousRepository;
import org.alliancegenome.neo4j.view.OrthologView;

@RequestScoped
public class OrthologyController implements OrthologyRESTInterface {

    public static final String API_VERSION = "0.91";

    @Context
    private HttpServletRequest request;

    @Inject
    private OrthologyCacheRepository service;

    @Override
    public JsonResultResponse<OrthologView> getDoubleSpeciesOrthology(String taxonIDOne,
                                                                      String taxonIDTwo,
                                                                      String stringency,
                                                                      String method,
                                                                      int limit,
                                                                      int page) {

        LocalDateTime startDate = LocalDateTime.now();
        Pagination pagination = new Pagination(page, limit, null, null);
        pagination.addFieldFilter(FieldFilter.STRINGENCY, stringency);
        pagination.addFieldFilter(FieldFilter.ORTHOLOGY_METHOD, method);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }

        JsonResultResponse<OrthologView> response = service.getOrthologyByTwoSpecies(taxonIDOne, taxonIDTwo, pagination);
        response.calculateRequestDuration(startDate);
        response.setApiVersion(API_VERSION);
        response.setHttpServletRequest(request);
        return response;
    }

    @Override
    public JsonResultResponse<OrthologView> getSingleSpeciesOrthology(String species,
                                                                      String stringencyFilter,
                                                                      String methods,
                                                                      int limit,
                                                                      int page) {
        LocalDateTime startDate = LocalDateTime.now();
        Pagination pagination = new Pagination(page, limit, null, null);
        pagination.addFieldFilter(FieldFilter.STRINGENCY, stringencyFilter);
        pagination.addFieldFilter(FieldFilter.ORTHOLOGY_METHOD, methods);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }

        JsonResultResponse<OrthologView> response = service.getOrthologyBySpecies(species, pagination);
        response.calculateRequestDuration(startDate);
        response.setApiVersion(API_VERSION);
        response.setHttpServletRequest(request);
        return response;
    }

    @Override
    public JsonResultResponse<OrthologView> getMultiSpeciesOrthology(List<String> taxonID, String taxonIdList, String stringencyFilter, String methods, Integer rows, Integer start) throws IOException {
        JsonResultResponse<OrthologView> response = new JsonResultResponse<OrthologView>();
        response.setNote("Not yet implemented");
        response.setApiVersion(API_VERSION);
        return response;
    }

    @Override
    public JsonResultResponse<OrthologView> getMultiGeneOrthology(List<String> geneIDs,
                                                                  String geneList,
                                                                  String stringencyFilter,
                                                                  String method,
                                                                  int rows,
                                                                  int page) {
        GeneController controller = new GeneController();
        //controller.setRequest(request);
        return controller.getGeneOrthology(null, geneIDs, geneList, stringencyFilter, null, method, rows, page);
    }

    @Override
    public JsonResultResponse<OrthoAlgorithm> getAllMethodsCalculations() {
        LocalDateTime startDate = LocalDateTime.now();
        OrthologousRepository orthoRepo = new OrthologousRepository();
        JsonResultResponse<OrthoAlgorithm> response = new JsonResultResponse<>();
        List<OrthoAlgorithm> methodList = orthoRepo.getAllMethods();
        response.setResults(methodList);
        response.setTotal(methodList.size());
        response.setApiVersion(API_VERSION);
        response.calculateRequestDuration(startDate);
        return response;
    }
}
