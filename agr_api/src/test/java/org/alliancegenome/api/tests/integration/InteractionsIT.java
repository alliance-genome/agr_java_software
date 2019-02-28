package org.alliancegenome.api.tests.integration;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.neo4j.entity.EntitySummary;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;


@Api(value = "Interaction Tests")
public class InteractionsIT {

    public static InteractionRepository repo = new InteractionRepository();
    private GeneService geneService = new GeneService();

    public static void main(String[] args) throws Exception {
        Logger log = LogManager.getLogger(InteractionsIT.class);

        //List<InteractionGeneJoin> list = repo.getInteractions("MGI:99604");
        //List<InteractionGeneJoin> list = repo.getInteractions("MGI:97490");
        //List<InteractionGeneJoin> list = repo.getInteractions("WB:WBGene00003912");
        //MGI:103150
        List<InteractionGeneJoin> list = repo.getInteractions("MGI:103150"); // 135, 116
        //List<InteractionGeneJoin> list = repo.getInteractions("HGNC:11998");


        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setSerializationInclusion(Include.NON_NULL);

        String json = mapper.writerWithView(View.Interaction.class).writeValueAsString(list);

        log.info("Json: " + json);
        log.info("Count: " + list.size());
        for (InteractionGeneJoin join : list) {
            log.info(join);
        }
    }

    @Test
    public void getInteractionSummary() {
        EntitySummary summary = geneService.getInteractionSummary("MGI:109583");
        assertNotNull(summary);
        assertThat(summary.getNumberOfAnnotations(), equalTo(34L));
        assertThat(summary.getNumberOfEntities(), equalTo(18L));

        summary = geneService.getInteractionSummary("HGNC:4601");
        assertNotNull(summary);
        assertThat(summary.getNumberOfAnnotations(), equalTo(183L));
        assertThat(summary.getNumberOfEntities(), equalTo(95L));

    }
}
