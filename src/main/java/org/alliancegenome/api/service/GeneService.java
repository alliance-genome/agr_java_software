package org.alliancegenome.api.service;

import org.alliancegenome.es.index.site.dao.GeneDAO;

import javax.enterprise.context.RequestScoped;
import java.util.Map;

@RequestScoped
public class GeneService {

    private static GeneDAO geneDAO = new GeneDAO();

    public Map<String, Object> getById(String id) {
        Map<String, Object> geneMap = geneDAO.getById(id);
        // if not found directly check if it is a secondary id on a different gene
        if (geneMap == null) {
            return geneDAO.getGeneBySecondary(id);
        }
        return geneMap;
    }

}
