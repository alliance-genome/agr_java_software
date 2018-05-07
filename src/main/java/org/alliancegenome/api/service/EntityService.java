package org.alliancegenome.api.service;

import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.es.index.site.dao.EntityDAO;

@RequestScoped
public class EntityService {

    private static EntityDAO entityDAO = new EntityDAO();

    public Map<String, Object> getById(String id) {
        return entityDAO.getById(id);
    }

}
