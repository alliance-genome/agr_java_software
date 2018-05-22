package org.alliancegenome.api.controller;

import org.alliancegenome.api.rest.interfaces.EntityRESTInterface;
import org.alliancegenome.api.service.EntityService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Map;

@RequestScoped
public class EntityController implements EntityRESTInterface {

    @Inject
    private EntityService entityService;

    @Override
    public Map<String, Object> getEntity(String id) {
        return entityService.getById(id);
    }

}
