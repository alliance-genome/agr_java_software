package org.alliancegenome.api.controller;

import org.alliancegenome.api.rest.interfaces.DiseaseRESTInterface;
import org.alliancegenome.api.service.APIService;
import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static org.alliancegenome.api.service.EntityType.DISEASE;
import static org.alliancegenome.api.service.EntityType.GENE;

@RequestScoped
public class DiseaseController extends BaseController implements DiseaseRESTInterface {

    private final Logger log = LogManager.getLogger(getClass());
    @Context  //injected response proxy supporting multiple threads
    private HttpServletResponse response;
    @Inject
    private HttpServletRequest request;

    @Inject
    private DiseaseService diseaseService;
    private final DiseaseAnnotationToTdfTranslator translator = new DiseaseAnnotationToTdfTranslator();


    @Override
    public DOTerm getDisease(String id) {
        DOTerm doTerm = diseaseService.getById(id);
        if (doTerm == null) {
            RestErrorMessage error = new RestErrorMessage("No disease term found with ID: " + id);
            throw new RestErrorException(error);
        } else {
            return doTerm;
        }
    }

    @Override
    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsSorted(String id,
                                                                             int limit,
                                                                             int page,
                                                                             String sortBy,
                                                                             String geneName,
                                                                             String species,
                                                                             String geneticEntity,
                                                                             String geneticEntityType,
                                                                             String disease,
                                                                             String source,
                                                                             String reference,
                                                                             String evidenceCode,
                                                                             String basedOnGeneSymbol,
                                                                             String associationType,
                                                                             String asc) {
        long startTime = System.currentTimeMillis();
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.GENE_NAME, geneName);
        pagination.addFieldFilter(FieldFilter.SPECIES, species);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY, geneticEntity);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
        pagination.addFieldFilter(FieldFilter.DISEASE, disease);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
        pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, evidenceCode);
        pagination.addFieldFilter(FieldFilter.BASED_ON_GENE, basedOnGeneSymbol);
        pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, associationType);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }
        try {
            JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsByDisease(id, pagination);
            response.setHttpServletRequest(request);
            response.calculateRequestDuration(startTime);

            return response;
        } catch (Exception e) {
            log.error("Error while retrieving disease annotations", e);
            RestErrorMessage error = new RestErrorMessage();
            error.addErrorMessage(e.getMessage());
            throw new RestErrorException(error);
        }
    }

    @Override
    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsByAllele(String id,
                                                                               int limit,
                                                                               int page,
                                                                               String sortBy,
                                                                               String geneName,
                                                                               String alleleName,
                                                                               String species,
                                                                               String disease,
                                                                               String source,
                                                                               String reference,
                                                                               String evidenceCode,
                                                                               String associationType,
                                                                               String asc) {
        long startTime = System.currentTimeMillis();
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.GENE_NAME, geneName);
        pagination.addFieldFilter(FieldFilter.ALLELE, alleleName);
        pagination.addFieldFilter(FieldFilter.SPECIES, species);
        pagination.addFieldFilter(FieldFilter.DISEASE, disease);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
        pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, evidenceCode);
        pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, associationType);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }
        try {
            JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithAlleles(id, pagination);
            response.setHttpServletRequest(request);
            response.calculateRequestDuration(startTime);

            return response;
        } catch (Exception e) {
            log.error("Error while retrieving disease annotations by allele", e);
            RestErrorMessage error = new RestErrorMessage();
            error.addErrorMessage(e.getMessage());
            throw new RestErrorException(error);
        }
    }

    @Override
    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsByGene(String id,
                                                                             int limit,
                                                                             int page,
                                                                             String sortBy,
                                                                             String geneName,
                                                                             String species,
                                                                             String disease,
                                                                             String source,
                                                                             String reference,
                                                                             String evidenceCode,
                                                                             String associationType,
                                                                             String asc) {
        long startTime = System.currentTimeMillis();
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.GENE_NAME, geneName);
        pagination.addFieldFilter(FieldFilter.SPECIES, species);
        pagination.addFieldFilter(FieldFilter.DISEASE, disease);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
        pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, evidenceCode);
        pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, associationType);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }
        try {
            JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithGenes(id, pagination);
            response.setHttpServletRequest(request);
            response.calculateRequestDuration(startTime);

            return response;
        } catch (Exception e) {
            log.error("Error while retrieving disease annotations by allele", e);
            RestErrorMessage error = new RestErrorMessage();
            error.addErrorMessage(e.getMessage());
            throw new RestErrorException(error);
        }
    }

    @Override
    public JsonResultResponse<PrimaryAnnotatedEntity> getDiseaseAnnotationsForModel(String id,
                                                                                    int limit,
                                                                                    int page,
                                                                                    String sortBy,
                                                                                    String modelName,
                                                                                    String geneName,
                                                                                    String species,
                                                                                    String disease,
                                                                                    String source,
                                                                                    String reference,
                                                                                    String evidenceCode,
                                                                                    String asc) {
        long startTime = System.currentTimeMillis();
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.GENE_NAME, geneName);
        pagination.addFieldFilter(FieldFilter.SPECIES, species);
        pagination.addFieldFilter(FieldFilter.DISEASE, disease);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
        pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, evidenceCode);
        pagination.addFieldFilter(FieldFilter.MODEL_NAME, modelName);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }
        try {
            JsonResultResponse<PrimaryAnnotatedEntity> response = diseaseService.getDiseaseAnnotationsWithAGM(id, pagination);
            response.setHttpServletRequest(request);
            response.calculateRequestDuration(startTime);

            return response;
        } catch (Exception e) {
            log.error("Error while retrieving disease annotations by allele", e);
            RestErrorMessage error = new RestErrorMessage();
            error.addErrorMessage(e.getMessage());
            throw new RestErrorException(error);
        }
    }

    @Override
    public Response getDiseaseAnnotationsDownloadFile(String id,
                                                      String sortBy,
                                                      String geneName,
                                                      String species,
                                                      String geneticEntity,
                                                      String geneticEntityType,
                                                      String disease,
                                                      String source,
                                                      String reference,
                                                      String evidenceCode,
                                                      String basedOnGeneSymbol,
                                                      String associationType,
                                                      String asc) {
        Pagination pagination = new Pagination(1, Integer.MAX_VALUE, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.GENE_NAME, geneName);
        pagination.addFieldFilter(FieldFilter.SPECIES, species);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY, geneticEntity);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
        pagination.addFieldFilter(FieldFilter.DISEASE, disease);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
        pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, evidenceCode);
        pagination.addFieldFilter(FieldFilter.BASED_ON_GENE, basedOnGeneSymbol);
        pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, associationType);
        Response.ResponseBuilder responseBuilder = null;
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }
        try {
            JsonResultResponse<DiseaseAnnotation> jsonResponse = diseaseService.getDiseaseAnnotationsByDisease(id, pagination);
            responseBuilder = Response.ok(translator.getAllRows(jsonResponse.getResults()));
            responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
            APIService.setDownloadHeader(id, DISEASE, GENE, responseBuilder);
        } catch (Exception e) {
            log.error(e);
            RestErrorMessage error = new RestErrorMessage();
            error.addErrorMessage(e.getMessage());
            throw new RestErrorException(error);
        }
        return responseBuilder.build();
    }

    @Override
    public String getDiseaseAnnotationsDownload(String id) {
        Pagination pagination = new Pagination(1, Integer.MAX_VALUE, null, null);
        // retrieve all records
        return translator.getAllRows(diseaseService.getDiseaseAnnotationsByDisease(id, pagination).getResults());
    }

    @Override
    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsRibbonDetails(List<String> geneIDs,
                                                                                    String termID,
                                                                                    String filterSpecies,
                                                                                    String filterGene,
                                                                                    String filterReference,
                                                                                    String diseaseTerm,
                                                                                    String filterSource,
                                                                                    String geneticEntity,
                                                                                    String geneticEntityType,
                                                                                    String associationType,
                                                                                    String evidenceCode,
                                                                                    String basedOnGeneSymbol,
                                                                                    int limit,
                                                                                    int page,
                                                                                    String sortBy,
                                                                                    String asc) {

        LocalDateTime startDate = LocalDateTime.now();
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        BaseFilter filterMap = new BaseFilter();
        filterMap.put(FieldFilter.SPECIES, filterSpecies);
        filterMap.put(FieldFilter.GENE_NAME, filterGene);
        filterMap.put(FieldFilter.FREFERENCE, filterReference);
        filterMap.put(FieldFilter.SOURCE, filterSource);
        filterMap.put(FieldFilter.DISEASE, diseaseTerm);
        filterMap.put(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
        filterMap.put(FieldFilter.GENETIC_ENTITY, geneticEntity);
        filterMap.put(FieldFilter.ASSOCIATION_TYPE, associationType);
        filterMap.put(FieldFilter.EVIDENCE_CODE, evidenceCode);
        filterMap.put(FieldFilter.BASED_ON_GENE, basedOnGeneSymbol);
        filterMap.values().removeIf(Objects::isNull);
        pagination.setFieldFilterValueMap(filterMap);

        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }
        try {
            JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(geneIDs, termID, pagination);
            response.setHttpServletRequest(request);
            response.calculateRequestDuration(startDate);
            return response;
        } catch (Exception e) {
            log.error("Error while retrieving disease annotations", e);
            RestErrorMessage error = new RestErrorMessage();
            error.addErrorMessage(e.getMessage());
            throw new RestErrorException(error);
        }
    }

    @Override
    public Response getDiseaseAnnotationsRibbonDetailsDownload(List<String> geneIDs,
                                                               String termID,
                                                               String filterSpecies,
                                                               String filterGene,
                                                               String filterReference,
                                                               String diseaseTerm,
                                                               String filterSource,
                                                               String geneticEntity,
                                                               String geneticEntityType,
                                                               String associationType,
                                                               String evidenceCode,
                                                               String basedOnGeneSymbol,
                                                               String sortBy,
                                                               String asc) {

        LocalDateTime startDate = LocalDateTime.now();
        Pagination pagination = new Pagination(1, Integer.MAX_VALUE, sortBy, asc);
        BaseFilter filterMap = new BaseFilter();
        filterMap.put(FieldFilter.SPECIES, filterSpecies);
        filterMap.put(FieldFilter.GENE_NAME, filterGene);
        filterMap.put(FieldFilter.FREFERENCE, filterReference);
        filterMap.put(FieldFilter.SOURCE, filterSource);
        filterMap.put(FieldFilter.DISEASE, diseaseTerm);
        filterMap.put(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
        filterMap.put(FieldFilter.GENETIC_ENTITY, geneticEntity);
        filterMap.put(FieldFilter.ASSOCIATION_TYPE, associationType);
        filterMap.put(FieldFilter.EVIDENCE_CODE, evidenceCode);
        filterMap.put(FieldFilter.BASED_ON_GENE, basedOnGeneSymbol);
        filterMap.values().removeIf(Objects::isNull);
        pagination.setFieldFilterValueMap(filterMap);
        Response.ResponseBuilder responseBuilder = null;
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }
        try {
            JsonResultResponse<DiseaseAnnotation> response = diseaseService.getRibbonDiseaseAnnotations(geneIDs, termID, pagination);
            response.setHttpServletRequest(request);
            response.calculateRequestDuration(startDate);
            // translate all records
            responseBuilder = Response.ok(translator.getAllRowsForRibbon(response.getResults()));
            responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
            APIService.setDownloadHeader(geneIDs.get(0), GENE, DISEASE, responseBuilder);
        } catch (Exception e) {
            log.error(e);
            RestErrorMessage error = new RestErrorMessage();
            error.addErrorMessage(e.getMessage());
            throw new RestErrorException(error);
        }

        return responseBuilder.build();
    }

}
