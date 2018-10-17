package org.alliancegenome.indexer;

import java.util.List;

import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InteractionsTest {

    public static InteractionRepository repo = new InteractionRepository();
    
    public static void main(String[] args) {
        Logger log = LogManager.getLogger(InteractionsTest.class);
        
        //List<InteractionGeneJoin> list = repo.getInteractions("MGI:99604");
        List<InteractionGeneJoin> list = repo.getInteractions("MGI:97490");
        log.info("Count: " + list.size());
        for(InteractionGeneJoin join: list) {
            log.info(join);
        }
    }
}
