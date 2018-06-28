package org.alliancegenome.api.service;

import org.alliancegenome.es.index.site.dao.GeneDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchResult;

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

    public SearchResult getAllelesByGene(String id) {
        // temporary fix until we allow full pagination, sorting and filtering
        Pagination pagination = new Pagination(1, 1000, null, null);
        return geneDAO.getAllelesByGene(id, pagination);
    }

    public SearchResult getPhenotypeAnnotations(String id, Pagination pagination) {
        return geneDAO.getPhenotypeAnnotations(id, pagination);
    }
}
