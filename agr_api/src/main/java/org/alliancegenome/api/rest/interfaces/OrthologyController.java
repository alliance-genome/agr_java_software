package org.alliancegenome.api.rest.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologyFilter;

import java.io.IOException;
import java.util.List;

public class OrthologyController implements OrthologyRESTInterface {

    @Override
    public JsonResultResponse getGeneOrthology(String speciesOne,
                                   String speciesTwo,
                                   String stringencyFilter,
                                   String methods,
                                   Integer rows,
                                   Integer start) throws IOException {

        GeneRepository repository = new GeneRepository();
        List<Gene> geneList = repository.getOrthologyByTwoSpecies(speciesOne, speciesTwo);
        OrthologyFilter orthologyFilter = new OrthologyFilter(stringencyFilter, null, methods);
        String json = OrthologyService.getOrthologyMultiGeneJson(geneList, orthologyFilter);
        ObjectMapper mapper = new ObjectMapper();
        JsonResultResponse response = mapper.readValue(json, JsonResultResponse.class);
        return response;
    }
}
