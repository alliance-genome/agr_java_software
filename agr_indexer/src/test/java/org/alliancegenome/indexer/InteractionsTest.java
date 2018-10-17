package org.alliancegenome.indexer;

import java.util.List;

import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.InteractionRepository;

public class InteractionsTest {

    public static InteractionRepository repo = new InteractionRepository();
    
    public static void main(String[] args) {
        List<InteractionGeneJoin> list = repo.getInteractions("MGI:99604");
        
        for(InteractionGeneJoin join: list) {
            System.out.println(join);
        }
    }

}
