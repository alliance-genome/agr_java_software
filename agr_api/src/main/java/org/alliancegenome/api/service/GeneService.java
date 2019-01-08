package org.alliancegenome.api.service;

import java.util.List;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.index.site.dao.GeneDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.repository.InteractionRepository;

import com.fasterxml.jackson.core.JsonProcessingException;

@RequestScoped
public class GeneService {

    private static GeneDAO geneDAO = new GeneDAO();
    private static GeneRepository geneRepo = new GeneRepository();
    private static InteractionRepository interRepo = new InteractionRepository();

    public Gene getById(String id) {
        Gene gene = geneRepo.getOneGene(id);
        // if not found directly check if it is a secondary id on a different gene
        if (gene == null) {
            // TODO implement this method to return something other then null
            return geneRepo.getGeneBySecondary(id);
        }
        return gene;
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
