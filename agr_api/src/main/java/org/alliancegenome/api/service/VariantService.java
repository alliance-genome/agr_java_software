package org.alliancegenome.api.service;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.repository.AlleleRepository;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class VariantService {

    private AlleleRepository alleleRepo = new AlleleRepository();

    public Allele getById(String id) {
        return alleleRepo.getAllele(id);
    }

    public JsonResultResponse<Transcript> getTranscriptsByVariant(String variantID, Pagination pagination) {
        Variant variant = alleleRepo.getVariant(variantID);

        JsonResultResponse<Transcript> response = new JsonResultResponse<>();
        if (variant != null) {
            response.setTotal(variant.getTranscriptList().size());
            response.setResults(variant.getTranscriptList());
        }
        return response;
    }

}
