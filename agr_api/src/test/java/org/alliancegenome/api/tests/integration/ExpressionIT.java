package org.alliancegenome.api.tests.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import org.alliancegenome.api.entity.RibbonSummary;
import org.alliancegenome.api.service.AlleleService;
import org.alliancegenome.api.service.ExpressionService;
import org.alliancegenome.cache.repository.ExpressionCacheRepository;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@Api(value = "Expression Tests")
public class ExpressionIT {

    private ObjectMapper mapper = new ObjectMapper();
    private AlleleService alleleService;
    private ExpressionCacheRepository repository = new ExpressionCacheRepository();
    private ExpressionService expressionService = new ExpressionService();

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
    public void checkAllExpressions() {
        Pagination pagination = new Pagination();
        pagination.setLimit(100000);
        List<String> geneIDs = new ArrayList<>();
        geneIDs.add("MGI:109583");

        PaginationResult<ExpressionDetail> response = repository.getExpressionAnnotations(geneIDs, "UBERON:0000924", pagination);
        System.out.println(response.getTotalNumber());
    }

    @Test
    // Test Pten from MGI for expression ribbon summary
    public void checkExpressionRibbonHeader() {
        RibbonSummary summary = expressionService.getExpressionRibbonSummary(List.of("MGI:109583"));
        assertNotNull(summary);
    }

    @Test
    // Test Pten from MGI for expression ribbon summary
    public void checkExpressionRibbonNumbers() {
        RibbonSummary summary = expressionService.getExpressionRibbonSummary(List.of("MGI:98834"));
        assertNotNull(summary);
    }

    @Test
    // Test Pten from MGI for expression ribbon summary
    public void checkExpressionRibbonGoTerms() {
        GeneRepository geneRepository = new GeneRepository();
        List<GOTerm> terms = geneRepository.getFullGoTermList();
        assertNotNull(terms);
        String termNames = terms.stream().map(GOTerm::getName).collect(Collectors.joining(","));
        assertTrue(termNames.contains("extracellular region"));


    }

    @Test
    // Test Pten from MGI for expression ribbon summary
    public void checkExpressionFiltering() {
        Pagination pagination = new Pagination();
        BaseFilter filter = new BaseFilter();
        filter.addFieldFilter(FieldFilter.SOURCE, "9913");
        pagination.setFieldFilterValueMap(filter);
        JsonResultResponse<ExpressionDetail> summary = expressionService.getExpressionDetails(List.of("WB:WBGene00000898"), null, pagination);
        assertNotNull(summary);


    }

    @Test
    public void checkExpressionAnatomy() {
        Pagination pagination = new Pagination();
        BaseFilter filter = new BaseFilter();
        //filter.addFieldFilter(FieldFilter.SOURCE, "9913");
        pagination.setFieldFilterValueMap(filter);
        JsonResultResponse<ExpressionDetail> summary = expressionService.getExpressionDetails(List.of("ZFIN:ZDB-GENE-030131-845"), "UBERON:0001062", pagination);
        assertNotNull(summary);
        assertEquals(summary.getTotal(), 53);
    }

}