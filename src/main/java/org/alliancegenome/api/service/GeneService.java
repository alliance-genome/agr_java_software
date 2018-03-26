package org.alliancegenome.api.service;

import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.es.index.site.dao.GeneDAO;

@RequestScoped
public class GeneService {

    private static GeneDAO geneDAO = new GeneDAO();

    public Map<String, Object> getById(String id) {
        return geneDAO.getById(id);
    }

}
