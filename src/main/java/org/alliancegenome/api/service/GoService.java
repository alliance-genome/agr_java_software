package org.alliancegenome.api.service;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.shared.es.dao.site_index.GoDAO;

@RequestScoped
public class GoService {
    
    @Inject
    private GoDAO goDAO;

    public Map<String, Object> getById(String id) {
        return goDAO.getById(id);
    }

    
}
