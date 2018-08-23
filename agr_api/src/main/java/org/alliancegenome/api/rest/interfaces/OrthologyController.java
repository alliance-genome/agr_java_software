package org.alliancegenome.api.rest.interfaces;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.repository.OrthologousRepository;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OrthologyController implements OrthologyRESTInterface {

    @Override
    public String getDoubleSpeciesOrthology(String taxonIDOne,
                                            String taxonIDTwo,
                                            String stringencyFilter,
                                            List<String> methods,
                                            Integer rows,
                                            Integer start) throws IOException {

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
