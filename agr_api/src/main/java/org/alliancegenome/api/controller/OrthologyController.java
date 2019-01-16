package org.alliancegenome.api.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.api.rest.interfaces.OrthologyRESTInterface;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.OrthoAlgorithm;
import org.alliancegenome.neo4j.repository.OrthologousRepository;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.OrthologyFilter;

import com.fasterxml.jackson.core.JsonProcessingException;

public class OrthologyController implements OrthologyRESTInterface {

    public static final String API_VERSION = "0.9";

    @Override
    public JsonResultResponse<OrthologView> getDoubleSpeciesOrthology(String taxonIDOne,
                                            String taxonIDTwo,
                                            String stringencyFilter,
                                            String methods,
                                            Integer rows,
                                            Integer start) throws IOException {

        LocalDateTime startDate = LocalDateTime.now();
        OrthologousRepository orthoRepo = new OrthologousRepository();
        List<String> methodList = new ArrayList<>();
        methodList.add(methods);
        OrthologyFilter orthologyFilter = new OrthologyFilter(stringencyFilter, null, methodList);

        if (rows != null)
            orthologyFilter.setRows(rows);
        if (start != null)
            orthologyFilter.setStart(start);

        JsonResultResponse<OrthologView> response;
        response = orthoRepo.getOrthologyByTwoSpecies(taxonIDOne, taxonIDTwo, orthologyFilter);
        response.calculateRequestDuration(startDate);
        response.setApiVersion(API_VERSION);
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
