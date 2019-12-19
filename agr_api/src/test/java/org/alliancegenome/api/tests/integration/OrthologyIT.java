package org.alliancegenome.api.tests.integration;

import io.swagger.annotations.Api;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologView;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.*;


@Api(value = "Orthology Tests")
@Log4j2
public class OrthologyIT {

    public static GeneRepository repo = new GeneRepository();
    OrthologyService service = new OrthologyService();

    public static void main(String[] args) {
    }

    public void getAllOrthologyGeneJoin() {

        MultiKeyMap map = repo.getAllOrthologyGeneJoin();

        assertNotNull(map);
    }

    @Test
    public void getGeneHomology() {
        OrthologyService service = new OrthologyService();
        Pagination pagination = new Pagination();
        pagination.setLimit(10);
        JsonResultResponse<OrthologView> response = service.getOrthologyMultiGeneJson(List.of("MGI:109583"), pagination);
        assertNotNull(response);
        assertThat(response.getTotal(), greaterThan(6));

        response = service.getOrthologyBySpecies("danio", pagination);
        assertNotNull(response);
        assertThat(response.getTotal(), greaterThan(6));
        // all source genes are ZFIN genes
        response.getResults().forEach(view -> assertEquals(view.getGene().getTaxonId(), "NCBITaxon:7955"));
    }

    @Test
    public void getSpeciesSpeciesOrthology() {
        OrthologyService service = new OrthologyService();
        Pagination pagination = new Pagination();
        pagination.setLimit(500);
        JsonResultResponse<OrthologView> response = service.getOrthologyByTwoSpecies("NCBITaxon:7955", "NCBITaxon:10090", pagination);
        assertNotNull(response);
        assertThat(response.getTotal(), greaterThan(86000));
        // all source genes are ZFIN genes
        response.getResults().forEach(view -> assertEquals(view.getGene().getTaxonId(), "NCBITaxon:7955"));

        String allGeneSymbolsConcat = String.join(",", response.getResults().stream().map(view -> view.getGene().getSymbol()).collect(Collectors.toList()));
        String allGeneSymbolsConcatSorted = String.join(",", response.getResults().stream().map(view -> view.getGene().getSymbol()).sorted().collect(Collectors.toList()));
        assertEquals(allGeneSymbolsConcat, allGeneSymbolsConcatSorted);

        pagination.addFieldFilter(FieldFilter.STRINGENCY, "stringent");
        response = service.getOrthologyByTwoSpecies("NCBITaxon:7955", "NCBITaxon:10090", pagination);
        assertThat(response.getTotal(), greaterThan(22000));
        assertThat(response.getTotal(), Matchers.lessThan(30000));

        // make sure the species are supported by partial names as well.
        response = service.getOrthologyByTwoSpecies("danio", "mus", pagination);
        assertNotNull(response);
        assertThat(response.getTotal(), greaterThan(1000));

        // make sure the species are supported by partial names as well.
        pagination.addFieldFilter(FieldFilter.ORTHOLOGY_METHOD, "roundUP");
        response = service.getOrthologyByTwoSpecies("danio", "mus", pagination);
        assertNotNull(response);
        assertThat(response.getTotal(), greaterThan(8000));
        assertThat(response.getTotal(), lessThan(11000));

    }
}
