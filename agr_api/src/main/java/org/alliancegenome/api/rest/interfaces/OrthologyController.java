package org.alliancegenome.api.rest.interfaces;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.repository.OrthologousRepository;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.alliancegenome.neo4j.view.View;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrthologyController implements OrthologyRESTInterface {

    public static final String API_VERSION = "0.9";

    @Context
    private HttpServletRequest request;

    @Override
    public String getDoubleSpeciesOrthology(String taxonIDOne,
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
        JsonResultResponse<OrthologView> response = null;
        response = orthoRepo.getOrthologyByTwoSpecies(taxonIDOne, taxonIDTwo, orthologyFilter);

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        response.calculateRequestDuration(startDate);
        response.setApiVersion(API_VERSION);
        response.setHttpServletRequest(request);
        return mapper.writerWithView(View.OrthologyView.class).writeValueAsString(response);
    }

    @Override
    public String getSingleSpeciesOrthology(String species,
                                            String stringencyFilter,
                                            String methods,
                                            Integer rows,
                                            Integer start) throws IOException {
        return getDoubleSpeciesOrthology(species, null, stringencyFilter, methods, rows, start);
    }
}
