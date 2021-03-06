package org.alliancegenome.api.controller;

import static org.alliancegenome.api.service.EntityType.*;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.*;

import org.alliancegenome.api.application.RestDefaultObjectMapper;
import org.alliancegenome.api.rest.interfaces.DiseaseRESTInterface;
import org.alliancegenome.api.service.*;
import org.alliancegenome.api.service.helper.APIServiceHelper;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.cache.repository.helper.SortingField;
import org.alliancegenome.core.exceptions.*;
import org.alliancegenome.core.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.core.util.FileHelper;
import org.alliancegenome.es.model.query.*;
import org.alliancegenome.neo4j.entity.*;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.view.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RequestScoped
public class DiseaseController implements DiseaseRESTInterface {

    @Inject
    private HttpServletRequest request;

    @Inject
    RestDefaultObjectMapper mapper;

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
                                                                             Integer limit,
                                                                             Integer page,
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
                                                                               Integer limit,
                                                                               Integer page,
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
        // The @DefaultValue only kicks in if the value is null.
        // need to handle an empty value manually here.
        if (sortBy.trim().isEmpty())
            sortBy = SortingField.DISEASE_ALLELE_DEFAULT.toString();
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
    public Response getDiseaseAnnotationsByAlleleDownload(String id,
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

        JsonResultResponse<DiseaseAnnotation> response = getDiseaseAnnotationsByAllele(id,
                Integer.MAX_VALUE,
                null,
                sortBy,
                geneName,
                alleleName,
                species,
                disease,
                source,
                reference,
                evidenceCode,
                associationType,
                asc);
        Response.ResponseBuilder responseBuilder = Response.ok(translator.getAllRowsForAllele(response.getResults()));
        APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.ALLELE, responseBuilder);
        return responseBuilder.build();
    }

    @Override
    public Response getDiseaseAnnotationsByGeneDownload(String id,
                                                        String sortBy,
                                                        String geneName,
                                                        String species,
                                                        String disease,
                                                        String source,
                                                        String reference,
                                                        String evidenceCode,
                                                        String basedOnGeneSymbol,
                                                        String associationType,
                                                        boolean fullDownload,
                                                        String downloadFileType,
                                                        String asc) {
        JsonResultResponse<DiseaseAnnotation> response = getDiseaseAnnotationsByGene(id,
                Integer.MAX_VALUE,
                null,
                sortBy,
                geneName,
                species,
                disease,
                source,
                reference,
                evidenceCode,
                basedOnGeneSymbol,
                associationType,
                asc);
        Response.ResponseBuilder responseBuilder = null;
        String allRowsForGenes = translator.getAllRowsForGenes(response.getResults());
        if (fullDownload) {
            if (downloadFileType == null || downloadFileType.equalsIgnoreCase("tsv")) {
                String data = FileHelper.getFileContent("templates/all-disease-association-file-header.txt");
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String dateString = format.format(new Date());
                data = data.replace("${date}", dateString);

                String taxonIDs = species;
                if (StringUtils.isEmpty(taxonIDs)) {
                    taxonIDs = SpeciesType.getAllTaxonIDs();
                }
                data = data.replace("${taxonIDs}", taxonIDs);
                data += allRowsForGenes;
                responseBuilder = Response.ok(data);
                APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.GENE, responseBuilder);
            } else if (downloadFileType.equalsIgnoreCase("JSON")) {
                try {
                    String data = mapper.getMapper().writerWithView(View.DiseaseAnnotationSummary.class).writeValueAsString(response);
                    responseBuilder = Response.ok(data);
                    APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.GENE, responseBuilder);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            } else {
                responseBuilder = Response.ok("The file type [" + downloadFileType + "] is not supported. Please use tsv or JSON");
                APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.GENE, responseBuilder);
            }
        } else {
            responseBuilder = Response.ok(allRowsForGenes);
            APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.GENE, responseBuilder);
        }
        return responseBuilder.build();

    }

    @Override
    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsByGene(String id,
                                                                             Integer limit,
                                                                             Integer page,
                                                                             String sortBy,
                                                                             String geneName,
                                                                             String species,
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
    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsForModel(String id,
                                                                               Integer limit,
                                                                               Integer page,
                                                                               String sortBy,
                                                                               String modelName,
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
        pagination.addFieldFilter(FieldFilter.MODEL_NAME, modelName);
        pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, associationType);
        if (pagination.hasErrors()) {
            RestErrorMessage message = new RestErrorMessage();
            message.setErrors(pagination.getErrors());
            throw new RestErrorException(message);
        }
        try {
            JsonResultResponse<DiseaseAnnotation> response = diseaseService.getDiseaseAnnotationsWithAGM(id, pagination);
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
    public Response getDiseaseAnnotationsForModelDownload(String id,
                                                          String sortBy,
                                                          String modelName,
                                                          String geneName,
                                                          String species,
                                                          String disease,
                                                          String source,
                                                          String reference,
                                                          String evidenceCode,
                                                          String associationType,
                                                          String asc) {
        JsonResultResponse<DiseaseAnnotation> response = getDiseaseAnnotationsForModel(id,
                Integer.MAX_VALUE,
                null,
                sortBy,
                modelName,
                geneName,
                species,
                disease,
                source,
                reference,
                evidenceCode,
                associationType,
                asc);
        Response.ResponseBuilder responseBuilder = Response.ok(translator.getAllRowsForModel(response.getResults()));
        APIServiceHelper.setDownloadHeader(id, EntityType.DISEASE, EntityType.MODEL, responseBuilder);
        return responseBuilder.build();
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
            responseBuilder = Response.ok(translator.getAllRowsForGenes(jsonResponse.getResults()));
            responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
            APIServiceHelper.setDownloadHeader(id, DISEASE, GENE, responseBuilder);
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
        return translator.getAllRowsForGenes(diseaseService.getDiseaseAnnotationsByDisease(id, pagination).getResults());
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
                                                                                    String includeNegation,
                                                                                    Integer limit,
                                                                                    Integer page,
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
        filterMap.put(FieldFilter.INCLUDE_NEGATION, includeNegation);
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
                                                               String includeNegation,
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
        filterMap.put(FieldFilter.INCLUDE_NEGATION, includeNegation);
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
            responseBuilder = Response.ok(translator.getAllRowsForGenes(response.getResults()));
            responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
            APIServiceHelper.setDownloadHeader(geneIDs.get(0), GENE, DISEASE, responseBuilder);
        } catch (Exception e) {
            log.error(e);
            RestErrorMessage error = new RestErrorMessage();
            error.addErrorMessage(e.getMessage());
            throw new RestErrorException(error);
        }

        return responseBuilder.build();
    }

    @Override
    public Response getDiseaseAnnotationsBySpeciesDownload(List<String> speciesIDs, String diseaseID, String sortBy) {

        if (CollectionUtils.isEmpty(speciesIDs)) {
            speciesIDs = Arrays.stream(SpeciesType.values()).map(SpeciesType::getTaxonID).collect(Collectors.toList());
        } else {
            speciesIDs = speciesIDs.stream().map(SpeciesType::getTaxonId).collect(Collectors.toList());
        }
        List<DiseaseAnnotation> alleleAnnotations = new ArrayList<>();
        List<DiseaseAnnotation> geneAnnotations = new ArrayList<>();
        List<DiseaseAnnotation> modelAnnotations = new ArrayList<>();
        speciesIDs.forEach(species -> {
            alleleAnnotations.addAll(getDiseaseAnnotationsByAllele(diseaseID,
                    Integer.MAX_VALUE,
                    null,
                    sortBy,
                    null,
                    null,
                    species,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null).getResults());

            modelAnnotations.addAll(getDiseaseAnnotationsForModel(diseaseID,
                    Integer.MAX_VALUE,
                    null,
                    sortBy,
                    null,
                    null,
                    species,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null).getResults());

            geneAnnotations.addAll(getDiseaseAnnotationsByGene(diseaseID,
                    Integer.MAX_VALUE,
                    null,
                    sortBy,
                    null,
                    species,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null).getResults());
        });
        Response.ResponseBuilder responseBuilder = Response.ok(translator.getAllRowsForGenesAndAlleles(geneAnnotations,
                alleleAnnotations,
                modelAnnotations));
        return responseBuilder.build();
    }

}
