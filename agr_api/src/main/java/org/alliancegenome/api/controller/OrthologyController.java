package org.alliancegenome.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.alliancegenome.api.rest.interfaces.OrthologyRESTInterface;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.OrthoAlgorithm;
import org.alliancegenome.neo4j.repository.OrthologousRepository;
import org.alliancegenome.neo4j.view.OrthologView;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class OrthologyController implements OrthologyRESTInterface {

    public static final String API_VERSION = "0.91";

    @Context
    private HttpServletRequest request;

    private OrthologyService service = new OrthologyService();

    @Override
    public JsonResultResponse<OrthologView> getDoubleSpeciesOrthology(String taxonIDOne,
                                                                      String taxonIDTwo,
                                                                      String stringency,
                                                                      String method,
                                                                      Integer limit,
                                                                      Integer page) {

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
                                                                      Integer rows,
                                                                      Integer start) throws IOException {
        return getDoubleSpeciesOrthology(species, null, stringencyFilter, methods, rows, start);
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
                                                                  List<String> methods,
                                                                  Integer rows,
                                                                  Integer start) throws IOException {
        GeneController controller = new GeneController();
        //controller.setRequest(request);
        return controller.getGeneOrthology(null, geneIDs, geneList, stringencyFilter, null, methods, rows, start);
    }

    @Override
    public JsonResultResponse<OrthoAlgorithm> getAllMethodsCalculations() throws JsonProcessingException {
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
