package org.alliancegenome.api.tests.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import org.alliancegenome.api.service.AlleleService;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.repository.ExpressionCacheRepository;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

@Api(value = "Expression Tests")
public class ExpressionIT {

    private ObjectMapper mapper = new ObjectMapper();
    private AlleleService alleleService;
    private ExpressionCacheRepository repository = new ExpressionCacheRepository();

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


}