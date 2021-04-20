package org.alliancegenome.core.tests;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.View;
import org.junit.Test;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class TestDateConversion {

    @Test
    public void testDataConversion() throws Exception {
        System.setProperty("NEO4J_HOST", "build.alliancegenome.org");
        
        GeneRepository repo = new GeneRepository();

        Gene g = repo.getOneGene("RGD:7559696");
        
        System.out.println(g.getDateProduced());
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        if (!ConfigHelper.isProduction())
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        
        
        System.out.println(mapper.writerWithView(View.GeneAPI.class).writeValueAsString(g));
        
        // "2020-05-27T19:23:23.882-05:00"
        //repo.getOneGene("2020-05-27T19:23:23.882-05:00");
    }
}
