package org.alliancegenome.api.service;

import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.es.index.site.dao.AlleleDAO;

@RequestScoped
public class AlleleService {
    
    private static AlleleDAO alleleDAO = new AlleleDAO();

    public Map<String, Object> getById(String id) {
        return alleleDAO.getById(id);
    }
    
}
