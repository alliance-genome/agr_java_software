package org.alliancegenome.api.service;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.repository.GoRepository;

@RequestScoped
public class GoService {

    private static GoRepository goRepo = new GoRepository();

    private GoService() {} // Cannot be instantiated needs to be @Injected
    
    public GOTerm getById(String id) {
        return goRepo.getOneGoTerm(id);
    }
    
}
