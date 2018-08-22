package org.alliancegenome.api.rest.interfaces;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OrthologyController implements OrthologyRESTInterface {

    @Override
    public String getDoubleSpeciesOrthology(String speciesOne,
                                            String speciesTwo,
                                            String stringencyFilter,
                                            List<String> methods,
                                            Integer rows,
                                            Integer start) throws IOException {

        GeneRepository repository = new GeneRepository();
        Set<Gene> geneList = null;
        if (speciesTwo != null)
            geneList = repository.getOrthologyByTwoSpecies(speciesOne, speciesTwo);
        else
            geneList = repository.getOrthologyBySingleSpecies(speciesOne);

        OrthologyFilter orthologyFilter = new OrthologyFilter(stringencyFilter, null, methods);
        JsonResultResponse<OrthologView> response = OrthologyService.getOrthologyMultiGeneJson(geneList, orthologyFilter);
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
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
