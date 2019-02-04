package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.rest.interfaces.AlleleRESTInterface;
import org.alliancegenome.api.service.AlleleService;
import org.alliancegenome.neo4j.entity.node.Allele;

@RequestScoped
public class AlleleController implements AlleleRESTInterface {

    @Inject
    private AlleleService alleleService;

    @Override
    public Allele getAllele(String id) {
        return alleleService.getById(id);
    }

}
