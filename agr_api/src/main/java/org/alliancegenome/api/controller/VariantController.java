package org.alliancegenome.api.controller;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.rest.interfaces.VariantRESTInterface;
import org.alliancegenome.api.service.VariantService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.alliancegenome.neo4j.entity.node.Variant;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Log4j2
@RequestScoped
public class VariantController implements VariantRESTInterface {

    @Inject
    private VariantService variantService;

    @Inject
    private HttpServletRequest request;

    @Override
    public Variant getVariant(String id) {
        Variant variant = variantService.getVariantById(id);
        if (variant == null) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(List.of("Cannot find variant with ID: " + id));
            throw new RestErrorException(message);
        }
        return variant;
    }

    @Override
    public JsonResultResponse<Transcript> getTranscriptsPerVariant(
            String id,
            Integer limit,
            Integer page,
            String sortBy,
            String transcriptType,
            String molecularConsequence) {

        long startTime = System.currentTimeMillis();
        Pagination pagination = new Pagination(page, limit, sortBy, null);
        pagination.addFieldFilter(FieldFilter.VARIANT_TYPE, transcriptType);
        pagination.addFieldFilter(FieldFilter.MOLECULAR_CONSEQUENCE, molecularConsequence);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }
        try {
            JsonResultResponse<Transcript> alleles = variantService.getTranscriptsByVariant(id, pagination);
            alleles.setHttpServletRequest(request);
            alleles.calculateRequestDuration(startTime);
            return alleles;
        } catch (Exception e) {
            log.error("Error while retrieving variant info", e);
            RestErrorMessage error = new RestErrorMessage();
            error.addErrorMessage(e.getMessage());
            throw new RestErrorException(error);
        }
    }

    @Override
    public JsonResultResponse<Allele> getAllelesPerVariant(String id,
                                                           Integer limit,
                                                           Integer page) {

        long startTime = System.currentTimeMillis();
        Pagination pagination = new Pagination(page, limit, null, null);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }
        try {
            JsonResultResponse<Allele> alleles = variantService.getAllelesByVariant(id, pagination);
            alleles.setHttpServletRequest(request);
            alleles.calculateRequestDuration(startTime);
            return alleles;
        } catch (Exception e) {
            log.error("Error while retrieving allele info", e);
            RestErrorMessage error = new RestErrorMessage();
            error.addErrorMessage(e.getMessage());
            throw new RestErrorException(error);
        }

    }

}
