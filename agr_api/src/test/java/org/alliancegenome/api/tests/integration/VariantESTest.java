package org.alliancegenome.api.tests.integration;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.VariantESDAO;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.junit.Assert.assertNotNull;


public class VariantESTest {

    private VariantESDAO variantESDAO = new VariantESDAO();

    @Before
    public void before() {
        Configurator.setRootLevel(Level.WARN);
        ConfigHelper.init();

        //alleleService = new AlleleService();

    }


    @Test
    public void checkAllelesBySpecies() {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder bool = boolQuery();

        bool.filter(new TermQueryBuilder("category", "allele"));
        bool.must(new TermQueryBuilder("variant.gene.id.keyword", "RGD:2219"));
        searchSourceBuilder.query(bool);
//        SearchSourceBuilder buil = searchSourceBuilder.aggregation(AggregationBuilders.terms("keys1").field("transcriptLevelConsequences.geneLevelConsequence.keyword"));

        Pagination pagination = new Pagination();
        pagination.addFieldFilter(FieldFilter.MOLECULAR_CONSEQUENCE, ".50");
        JsonResultResponse<Allele> list = variantESDAO.performQuery(searchSourceBuilder, new Pagination());
        assertNotNull(list);
    }

}
