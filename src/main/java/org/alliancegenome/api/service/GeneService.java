package org.alliancegenome.api.service;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.shared.es.dao.site_index.GeneDAO;

@RequestScoped
public class GeneService {

    @Inject
    private GeneDAO geneDAO;

    public Map<String, Object> getById(String id) {
        return geneDAO.getById(id);
    }

}
