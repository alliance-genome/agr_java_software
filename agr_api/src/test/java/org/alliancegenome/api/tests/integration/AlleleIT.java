package org.alliancegenome.api.tests.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import org.alliancegenome.api.service.AlleleService;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@Api(value = "Allele Tests")
public class AlleleIT {

    private ObjectMapper mapper = new ObjectMapper();
    private AlleleService alleleService;

    @Before
    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();

        alleleService = new AlleleService();

        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new OrthologyModule());
    }


    @Test
    @Ignore
    public void checkAllelesBySpecies() {
        Pagination pagination = new Pagination();
        pagination.setLimit(100000);
        JsonResultResponse<Allele> response = alleleService.getAllelesBySpecies("dani", pagination);
        System.out.println(response.getTotal());
        assertResponse(response, 40000, 40000);
    }

    @Test
    public void checkAllelesByGene() {
        Pagination pagination = new Pagination();
        JsonResultResponse<Allele> response = alleleService.getAllelesByGene("MGI:109583", pagination);
        assertResponse(response, 19, 19);
    }

    @Test
    public void checkAllelesWithDiseases() {
        Pagination pagination = new Pagination();
        JsonResultResponse<Allele> response = alleleService.getAllelesByGene("ZFIN:ZDB-GENE-040426-1716", pagination);
        List<Allele> term = response.getResults().stream().filter(allele -> allele.getDiseases() != null).collect(Collectors.toList());
        assertThat(term.size(), greaterThanOrEqualTo(1));
    }

    @Test
    @Ignore
    public void checkAlleles() {
        AlleleRepository repository = new AlleleRepository();
        Set<Allele> response = repository.getAllAlleles();
        assertThat(response.size(), greaterThanOrEqualTo(10));
    }

    private void assertResponse(JsonResultResponse<Allele> response, int resultSize, int totalSize) {
        assertNotNull(response);
        assertThat("Number of returned records", response.getResults().size(), greaterThanOrEqualTo(resultSize));
        assertThat("Number of total records", response.getTotal(), greaterThanOrEqualTo(totalSize));
    }


}