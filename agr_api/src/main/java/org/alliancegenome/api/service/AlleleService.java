package org.alliancegenome.api.service;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;

@RequestScoped
public class AlleleService {
    
    private static AlleleRepository alleleRepo = new AlleleRepository();

    public Allele getById(String id) {
        return alleleRepo.getAllele(id);
    }
    
}
