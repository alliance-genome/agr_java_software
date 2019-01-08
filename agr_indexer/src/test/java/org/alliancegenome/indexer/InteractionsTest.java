package org.alliancegenome.indexer;

import java.util.List;

import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class InteractionsTest {

    public static InteractionRepository repo = new InteractionRepository();
    
    public static void main(String[] args) throws Exception {
        Logger log = LogManager.getLogger(InteractionsTest.class);
        
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
        for(InteractionGeneJoin join: list) {
            log.info(join);
        }
    }
}
