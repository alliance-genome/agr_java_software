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
import java.util.List;

public class OrthologyController implements OrthologyRESTInterface {

    public static final String API_VERSION = "0.9";

    @Context
    private HttpServletRequest request;

    @Override
    public String getDoubleSpeciesOrthology(String taxonIDOne,
                                            String taxonIDTwo,
                                            String stringencyFilter,
                                            List<String> methods,
                                            Integer rows,
                                            Integer start) throws IOException {

        LocalDateTime startDate = LocalDateTime.now();
        OrthologousRepository orthoRepo = new OrthologousRepository();
        OrthologyFilter orthologyFilter = new OrthologyFilter(stringencyFilter, null, methods);
        orthologyFilter.setRows(rows);
        orthologyFilter.setStart(start);
        JsonResultResponse<OrthologView> response = null;
        if (taxonIDTwo != null)
            response = orthoRepo.getOrthologyByTwoSpecies(taxonIDOne, taxonIDTwo, orthologyFilter);
/*
        else
            geneList = repository.getOrthologyBySingleSpecies(taxonIDOne);
*/

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        response.setRequestDuration(startDate);
        response.setApiVersion(API_VERSION);
        response.setRequest(request);
        return mapper.writerWithView(View.OrthologyView.class).writeValueAsString(response);
    }

    @Override
    public String getSingleSpeciesOrthology(String species,
                                            String stringencyFilter,
                                            List<String> methods,
                                            Integer rows,
                                            Integer start) throws IOException {
        return getDoubleSpeciesOrthology(species, null, stringencyFilter, methods, rows, start);
    }
}
