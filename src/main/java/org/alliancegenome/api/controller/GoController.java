package org.alliancegenome.api.controller;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.rest.interfaces.GoRESTInterface;
import org.alliancegenome.api.service.GoService;

@RequestScoped
public class GoController implements GoRESTInterface {

    @Inject
    private GoService goService;
    
    @Override
    public Map<String, Object> getGo(String id) {
        return goService.getById(id);
    }

}
