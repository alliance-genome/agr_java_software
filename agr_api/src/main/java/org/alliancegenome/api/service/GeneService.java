package org.alliancegenome.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.index.site.dao.GeneDAO;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.InteractionRepository;

import javax.enterprise.context.RequestScoped;
import java.util.List;
import java.util.Map;

@RequestScoped
public class GeneService {

    private static GeneDAO geneDAO = new GeneDAO();
    private static InteractionRepository interRepo = new InteractionRepository();

    public Map<String, Object> getById(String id) {
        Map<String, Object> geneMap = geneDAO.getById(id);
        // if not found directly check if it is a secondary id on a different gene
        if (geneMap == null) {
            return geneDAO.getGeneBySecondary(id);
        }
        return geneMap;
    }

    public SearchApiResponse getAllelesByGene(String id) {
        // temporary fix until we allow full pagination, sorting and filtering
        Pagination pagination = new Pagination(1, 1000, null, null);
        return geneDAO.getAllelesByGene(id, pagination);
    }

    public JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotations(String id, Pagination pagination) throws JsonProcessingException {
        return geneDAO.getPhenotypeAnnotations(id, pagination);
    }

    public List<InteractionGeneJoin> getInteractions(String id) {
        return interRepo.getInteractions(id);
    }

}
