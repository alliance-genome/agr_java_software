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
    @Ignore
    public void sortByTranscript() {
        Pagination pagination = new Pagination();
        JsonResultResponse<Transcript> response = variantService.getTranscriptsByVariant("NC_000074.6:g.57322231_57322238delinsGAGGACGA", pagination);
        final List<Transcript> results = response.getResults();
        assertNotNull(results);
        assertThat(results.size(), greaterThan(3));
        String concatenatedGeneSYmbols = results.stream()
                .map(transcript -> transcript.getGene().getSymbol())
                .collect(Collectors.joining());
        String concatenatedGeneSymbolsSorted = results.stream()
                .map(transcript -> transcript.getGene().getSymbol())
                .sorted()
                .collect(Collectors.joining());

        assertEquals(concatenatedGeneSYmbols, concatenatedGeneSymbolsSorted);

    }

}
