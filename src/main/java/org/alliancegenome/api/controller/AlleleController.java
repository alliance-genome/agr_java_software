package org.alliancegenome.api.controller;

import org.alliancegenome.api.rest.interfaces.AlleleRESTInterface;
import org.alliancegenome.api.service.AlleleService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Map;

@RequestScoped
public class AlleleController implements AlleleRESTInterface {

    @Inject
    private AlleleService alleleService;

    @Override
    public Map<String, Object> getAllele(String id) {
        return alleleService.getById(id);
    }

}
