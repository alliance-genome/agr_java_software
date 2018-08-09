package org.alliancegenome.api.rest.interfaces;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologyFilter;

import java.io.IOException;
import java.util.List;

public class OrthologyController implements OrthologyRESTInterface {

    @Override
    public JsonResultResponse getDoubleSpeciesOrthology(String speciesOne,
                                                        String speciesTwo,
                                                        String stringencyFilter,
                                                        String methods,
                                                        Integer rows,
                                                        Integer start) throws IOException {

        GeneRepository repository = new GeneRepository();
        List<Gene> geneList = null;
        if (speciesTwo != null)
            geneList = repository.getOrthologyByTwoSpecies(speciesOne, speciesTwo);
        else
            geneList = repository.getOrthologyBySingleSpecies(speciesOne);

        OrthologyFilter orthologyFilter = new OrthologyFilter(stringencyFilter, null, methods);
        JsonResultResponse response = OrthologyService.getOrthologyMultiGeneJson(geneList, orthologyFilter);
        return response;
    }

    @Override
    public JsonResultResponse getSingleSpeciesOrthology(String species,
                                                        String stringencyFilter,
                                                        String methods,
                                                        Integer rows,
                                                        Integer start) throws IOException {
        return getDoubleSpeciesOrthology(species, null, stringencyFilter, methods, rows, start);
    }
}
