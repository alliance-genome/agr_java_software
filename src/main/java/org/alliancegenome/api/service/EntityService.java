package org.alliancegenome.api.service;

import org.alliancegenome.api.dao.EntityDAO;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Map;

@RequestScoped
public class EntityService {

    @Inject
    private EntityDAO entityDAO;

    public Map<String, Object> getById(String id) {
        return entityDAO.getById(id);
    }


}
