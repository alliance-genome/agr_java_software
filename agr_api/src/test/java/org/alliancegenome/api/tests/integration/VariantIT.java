package org.alliancegenome.api.tests.integration;

import org.alliancegenome.api.service.VariantService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class VariantIT {

    //    @Inject
    private VariantService variantService = new VariantService();

    @Before
    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();
    }


    @Test
    public void sortByTranscript() {
        Pagination pagination = new Pagination();
        JsonResultResponse<Transcript> response = variantService.getTranscriptsByVariant("NC_000074.6:g.57322231_57322238delinsGAGGACGA", pagination);
        final List<Transcript> results = response.getResults();
        assertNotNull(results);
        assertThat(results.size(), greaterThan(3));
        String concatenatedGeneSymbols = results.stream()
                .map(transcript -> transcript.getGene().getSymbol())
                .collect(Collectors.joining());
        String concatenatedGeneSymbolsSorted = results.stream()
                .map(transcript -> transcript.getGene().getSymbol())
                .sorted()
                .collect(Collectors.joining());

        assertEquals(concatenatedGeneSymbols, concatenatedGeneSymbolsSorted);
    }

    @Test
    public void getLocationExonTranscript() {
        Pagination pagination = new Pagination();
        JsonResultResponse<Transcript> response = variantService.getTranscriptsByVariant("NC_007126.7:g.15401132A>G", pagination);
        final List<Transcript> results = response.getResults();
        assertNotNull(results);
    }

    @Test
    public void paginateOverTranscripts() {
        Pagination pagination = new Pagination();
        pagination.setLimit(2);
        pagination.setPage(2);
        JsonResultResponse<Transcript> response = variantService.getTranscriptsByVariant("NC_000083.6:g.46025369_46025370delinsTC", pagination);
        List<Transcript> results = response.getResults();
        assertNotNull(results);
        assertThat(results.size(), equalTo(2));
        assertThat(response.getTotal(), greaterThan(200));

    }

}
