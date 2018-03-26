package org.alliancegenome.api.service;

import org.alliancegenome.api.dao.AlleleDAO;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Map;

@RequestScoped
public class AlleleService {
    
    @Inject
    private AlleleDAO alleleDAO;

    public Map<String, Object> getById(String id) {
        return alleleDAO.getById(id);
    }

    
}
