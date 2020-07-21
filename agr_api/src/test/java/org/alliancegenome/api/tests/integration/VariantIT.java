package org.alliancegenome.api.tests.integration;

import org.alliancegenome.api.service.VariantService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.repository.VariantRepository;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.*;

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
        List<Transcript> results = response.getResults();
        assertNotNull(results);
        VariantRepository variantRepo = new VariantRepository();

        // positive strand
        String variantID = "NC_005110.4:g.31653152_31653232del";
        Variant variant = variantRepo.getVariant(variantID);
        response = variantService.getTranscriptsByVariant(variantID, pagination);
        results = response.getResults();
        assertNotNull(results);
        Transcript transcript = results.get(0);
        variantService.populateIntronExonLocation(variant, transcript);
        assertEquals(transcript.getIntronExonLocation(), "Exon 3");

        // negative strand
        variantID = "NC_005120.4:g.154703661_154703782del";
        variant = variantRepo.getVariant(variantID);
        response = variantService.getTranscriptsByVariant(variantID, pagination);
        results = response.getResults();
        assertNotNull(results);
        transcript = results.get(0);
        variantService.populateIntronExonLocation(variant, transcript);
        assertEquals(transcript.getIntronExonLocation(), "Intron");

        variantID = "NC_005120.4:g.154703661_154703782del";
        variant = variantRepo.getVariant(variantID);
        response = variantService.getTranscriptsByVariant(variantID, pagination);
        results = response.getResults();
        assertNotNull(results);
        transcript = results.get(0);
        variantService.populateIntronExonLocation(variant, transcript);
        assertEquals(transcript.getIntronExonLocation(), "Intron");

        variantID = "NC_003279.8:g.152331C>A";
        variant = variantRepo.getVariant(variantID);
        response = variantService.getTranscriptsByVariant(variantID, pagination);
        results = response.getResults();
        assertNotNull(results);
        transcript = results.get(0);
        variantService.populateIntronExonLocation(variant, transcript);
        assertEquals(transcript.getIntronExonLocation(), "Exon 6");
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
