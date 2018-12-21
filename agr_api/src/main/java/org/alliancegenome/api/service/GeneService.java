package org.alliancegenome.api.service;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.index.site.dao.GeneDAO;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.repository.InteractionRepository;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

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

    public JsonResultResponse<Allele> getAlleles(String id) {
        JsonResultResponse<Allele> ret = new JsonResultResponse<Allele>();
        ret.setResults(geneRepo.getAlleles(id));
        return ret;
    }

    public JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotations(String id, Pagination pagination) throws JsonProcessingException {
        return geneDAO.getPhenotypeAnnotations(id, pagination);
    }

    public JsonResultResponse<InteractionGeneJoin> getInteractions(String id) {
        JsonResultResponse<InteractionGeneJoin> ret = new JsonResultResponse<InteractionGeneJoin>();
        ret.setResults(interRepo.getInteractions(id));
        return ret;
    }

    public JsonResultResponse<DiseaseAnnotation> getEmpiricalDiseaseAnnotations(String id, Pagination pagination, boolean empiricalDisease) throws JsonProcessingException {
        return geneDAO.getDiseaseAnnotations(id, pagination, empiricalDisease);
    }
/*

    public JsonResultResponse<DiseaseAnnotation> getEmpiricalDiseaseAnnotations(String id, Pagination pagination, boolean empiricalDisease) throws JsonProcessingException {
        return geneDAO.getEmpiricalDiseaseAnnotations(id, pagination, empiricalDisease);
    }
*/

}
