package org.alliancegenome.api.controller;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.rest.interfaces.AlleleRESTInterface;
import org.alliancegenome.api.service.APIService;
import org.alliancegenome.api.service.AlleleService;
import org.alliancegenome.api.service.EntityType;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.translators.tdf.AlleleToTdfTranslator;
import org.alliancegenome.core.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.core.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Variant;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

@Log4j2
@RequestScoped
public class AlleleController implements AlleleRESTInterface {

    @Inject
    private AlleleService alleleService;

    @Inject
    private HttpServletRequest request;

    private AlleleToTdfTranslator translator = new AlleleToTdfTranslator();
    private final PhenotypeAnnotationToTdfTranslator phenotypeAnnotationToTdfTranslator = new PhenotypeAnnotationToTdfTranslator();
    private final DiseaseAnnotationToTdfTranslator diseaseToTdfTranslator = new DiseaseAnnotationToTdfTranslator();

    @Override
    public Allele getAllele(String id) {
        return alleleService.getById(id);
    }

    @Override
    public JsonResultResponse<Variant> getVariantsPerAllele(String id,
                                                            int limit,
                                                            int page,
                                                            String sortBy,
                                                            String variantType,
                                                            String consequence) {
        long startTime = System.currentTimeMillis();
        Pagination pagination = new Pagination(page, limit, sortBy, null);
        pagination.addFieldFilter(FieldFilter.VARIANT_TYPE, variantType);
        pagination.addFieldFilter(FieldFilter.VARIANT_CONSEQUENCE, consequence);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }

        try {
            JsonResultResponse<Variant> alleles = alleleService.getVariants(id, pagination);
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
    public Response getVariantsPerAlleleDownload(String id,
                                                 String sortBy,
                                                 String variantType,
                                                 String consequence) {
        JsonResultResponse<Variant> response = getVariantsPerAllele(id,
                Integer.MAX_VALUE,
                1,
                sortBy,
                variantType,
                consequence);
        Response.ResponseBuilder responseBuilder = Response.ok(translator.getAllVariantsRows(response.getResults()));
        APIService.setDownloadHeader(id, EntityType.ALLELE, EntityType.VARIANT, responseBuilder);
        return responseBuilder.build();
    }

    @Override
    public JsonResultResponse<Allele> getAllelesPerSpecies(String species, int limit, int page, String sortBy, String asc) {
        long startTime = System.currentTimeMillis();
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        JsonResultResponse<Allele> response = alleleService.getAllelesBySpecies(species, pagination);
        response.setHttpServletRequest(request);
        Long duration = (System.currentTimeMillis() - startTime) / 1000;
        response.setRequestDuration(duration.toString());
        return response;
    }

    @Override
    public JsonResultResponse<PhenotypeAnnotation> getPhenotypePerAllele(String id,
                                                                         int limit,
                                                                         int page,
                                                                         String phenotype,
                                                                         String source,
                                                                         String reference,
                                                                         String sortBy) {
        long startTime = System.currentTimeMillis();
        Pagination pagination = new Pagination(page, limit, sortBy, null);
        pagination.addFieldFilter(FieldFilter.PHENOTYPE, phenotype);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }

        try {
            JsonResultResponse<PhenotypeAnnotation> phenotypeAnnotation = alleleService.getPhenotype(id, pagination);
            phenotypeAnnotation.setHttpServletRequest(request);
            phenotypeAnnotation.calculateRequestDuration(startTime);
            return phenotypeAnnotation;
        } catch (Exception e) {
            log.error("Error while retrieving phenotype info", e);
            RestErrorMessage error = new RestErrorMessage();
            error.addErrorMessage(e.getMessage());
            throw new RestErrorException(error);
        }
    }

    @Override
    public Response getPhenotypesPerAlleleDownload(String id,
                                                   String phenotype,
                                                   String source,
                                                   String reference,
                                                   String sortBy) {
        JsonResultResponse<PhenotypeAnnotation> response = getPhenotypePerAllele( id,
                Integer.MAX_VALUE,
                1,
         phenotype,
         source,
         reference,
         sortBy);
        Response.ResponseBuilder responseBuilder = Response.ok(phenotypeAnnotationToTdfTranslator.getAllRowsForAlleles(response.getResults()));
        APIService.setDownloadHeader(id, EntityType.ALLELE, EntityType.PHENOTYPE, responseBuilder);
        return responseBuilder.build();
    }

    public JsonResultResponse<DiseaseAnnotation> getDiseasePerAllele(String id,
                                                                     int limit,
                                                                     int page,
                                                                     String disease,
                                                                     String source,
                                                                     String reference,
                                                                     String sortBy) {
        long startTime = System.currentTimeMillis();
        Pagination pagination = new Pagination(page, limit, sortBy, null);
        pagination.addFieldFilter(FieldFilter.DISEASE, disease);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }

        try {
            JsonResultResponse<DiseaseAnnotation> alleles = alleleService.getDisease(id, pagination);
            alleles.setHttpServletRequest(request);
            alleles.calculateRequestDuration(startTime);
            return alleles;
        } catch (Exception e) {
            log.error("Error while retrieving disease info", e);
            RestErrorMessage error = new RestErrorMessage();
            error.addErrorMessage(e.getMessage());
            throw new RestErrorException(error);
        }
    }


    @Override
    public Response getDiseasePerAlleleDownload(String id,
                                                   String disease,
                                                   String source,
                                                   String reference,
                                                   String sortBy) {
        JsonResultResponse<DiseaseAnnotation> response = getDiseasePerAllele( id,
                Integer.MAX_VALUE,
                1,
                disease,
                source,
                reference,
                sortBy);
        Response.ResponseBuilder responseBuilder = Response.ok(diseaseToTdfTranslator.getAllRowsForAllele(response.getResults()));
        APIService.setDownloadHeader(id, EntityType.ALLELE, EntityType.DISEASE, responseBuilder);
        return responseBuilder.build();
    }

}
