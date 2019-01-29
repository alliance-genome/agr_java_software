package org.alliancegenome.api.controller;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.alliancegenome.api.rest.interfaces.GeneRESTInterface;
import org.alliancegenome.api.service.DiseaseService;
import org.alliancegenome.api.service.ExpressionService;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.api.service.helper.ExpressionSummary;
import org.alliancegenome.core.exceptions.RestErrorException;
import org.alliancegenome.core.exceptions.RestErrorMessage;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.core.translators.tdf.DiseaseAnnotationToTdfTranslator;
import org.alliancegenome.core.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@RequestScoped
public class GeneController extends BaseController implements GeneRESTInterface {

    @Inject
    private GeneService geneService;
    
    @Inject
    private DiseaseService diseaseService;

    private final PhenotypeAnnotationToTdfTranslator translator = new PhenotypeAnnotationToTdfTranslator();
    private final DiseaseAnnotationToTdfTranslator diseaseTranslator = new DiseaseAnnotationToTdfTranslator();
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public Gene getGene(String id) {
        Gene gene = geneService.getById(id);
        if (gene == null) {
            RestErrorMessage error = new RestErrorMessage("No gene found with ID: " + id);
            throw new RestErrorException(error);
        } else {

            return gene;
        }
    }
    
    @Override
    public JsonResultResponse<Allele> getAllelesPerGene(String id, int limit, int page, String sortBy, String asc) {
        return geneService.getAlleles(id, limit, page, sortBy, asc);
    }

    @Override
    public JsonResultResponse<InteractionGeneJoin> getInteractions(String id) {
        return geneService.getInteractions(id);
    }

    @Override
    public JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotations(String id, int limit, int page, String sortBy, String geneticEntity, String geneticEntityType, String phenotype, String reference, String asc) throws JsonProcessingException {
        return getPhenotypeAnnotationDocumentJsonResultResponse(id, limit, page, sortBy, geneticEntity, geneticEntityType, phenotype, reference, asc);
    }

    @Override
    public Response getPhenotypeAnnotationsDownloadFile(
            String id,
            String sortBy,
            String geneticEntity,
            String geneticEntityType,
            String phenotype,
            String reference,
            String asc) throws JsonProcessingException {
        // retrieve all records
        JsonResultResponse<PhenotypeAnnotation> response = getPhenotypeAnnotationDocumentJsonResultResponse(id, Integer.MAX_VALUE, 1, sortBy, geneticEntity, geneticEntityType, phenotype, reference, asc);
        Response.ResponseBuilder responseBuilder = Response.ok(translator.getAllRows(response.getResults()));
        responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
        responseBuilder.header("Content-Disposition", "attachment; filename=\"termName-annotations-" + id.replace(":", "-") + ".tsv\"");
        return responseBuilder.build();
    }

    private JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotationDocumentJsonResultResponse(String id, int limit, int page, String sortBy, String geneticEntity, String geneticEntityType, String phenotype, String reference, String asc) throws JsonProcessingException {
        if (sortBy.isEmpty())
            sortBy = FieldFilter.PHENOTYPE.getName();
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY, geneticEntity);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
        pagination.addFieldFilter(FieldFilter.PHENOTYPE, phenotype);
        pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
        return geneService.getPhenotypeAnnotations(id, pagination);
    }

    private JsonResultResponse<DiseaseAnnotation> getEmpiricalDiseaseAnnotation(String id,
                                                                                Integer limit,
                                                                                Integer page,
                                                                                String sortBy,
                                                                                String geneticEntity,
                                                                                String geneticEntityType,
                                                                                String disease,
                                                                                String associationType,
                                                                                String evidenceCode,
                                                                                String source,
                                                                                String reference,
                                                                                String asc) throws JsonProcessingException {
        return getDiseaseAnnotation(id, limit, page, sortBy, geneticEntity, geneticEntityType, disease, associationType, reference, null, null, evidenceCode, source, asc, true);
    }

    private JsonResultResponse<DiseaseAnnotation> getDiseaseViaOrthologyAnnotation(String id,
                                                                                   Integer limit,
                                                                                   Integer page,
                                                                                   String sortBy,
                                                                                   String orthologyGene,
                                                                                   String orthologyGeneSpecies,
                                                                                   String disease,
                                                                                   String associationType,
                                                                                   String evidenceCode,
                                                                                   String source,
                                                                                   String reference,
                                                                                   String asc) throws JsonProcessingException {
        return getDiseaseAnnotation(id, limit, page, sortBy, null, null, disease, associationType, reference, orthologyGene, orthologyGeneSpecies,evidenceCode, source, asc, false);
    }

    private JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotation(String id,
                                                                       Integer limit,
                                                                       Integer page,
                                                                       String sortBy,
                                                                       String geneticEntity,
                                                                       String geneticEntityType,
                                                                       String disease,
                                                                       String associationType,
                                                                       String reference,
                                                                       String orthologyGene,
                                                                       String orthologyGeneSpecies,
                                                                       String evidenceCode,
                                                                       String source,
                                                                       String asc,
                                                                       boolean empiricalDisease) throws JsonProcessingException {
        if (sortBy.isEmpty())
            sortBy = FieldFilter.PHENOTYPE.getName();
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY, geneticEntity);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
        pagination.addFieldFilter(FieldFilter.ASSOCIATION_TYPE, associationType);
        pagination.addFieldFilter(FieldFilter.EVIDENCE_CODE, evidenceCode);
        pagination.addFieldFilter(FieldFilter.SOURCE, source);
        pagination.addFieldFilter(FieldFilter.ORTHOLOG, orthologyGene);
        pagination.addFieldFilter(FieldFilter.ORTHOLOG_SPECIES, orthologyGeneSpecies);
        pagination.addFieldFilter(FieldFilter.DISEASE, disease);
        pagination.addFieldFilter(FieldFilter.FREFERENCE, reference);
        return diseaseService.getEmpiricalDiseaseAnnotations(id, pagination, empiricalDisease);
    }

    @Override
    public JsonResultResponse<OrthologView> getGeneOrthology(String id,
                                   List<String> geneIDs,
                                   String geneLister,
                                   String stringencyFilter,
                                   List<String> taxonIDs,
                                   List<String> methods,
                                   Integer rows,
                                   Integer start) throws IOException {

        List<String> geneList = new ArrayList<>();
        if (id != null) {
            geneList.add(id);
        }
        if (geneLister != null) {
            List<String> ids = Arrays.asList(geneLister.split(","));
            geneList.addAll(ids);
        }
        if (CollectionUtils.isNotEmpty(geneIDs)) {
            geneList.addAll(geneIDs);
        }
        OrthologyFilter orthologyFilter = new OrthologyFilter(stringencyFilter, taxonIDs, methods);
        if (rows != null && rows > 0) {
            orthologyFilter.setRows(rows);
        }
        orthologyFilter.setStart(start);
        return OrthologyService.getOrthologyMultiGeneJson(geneList, orthologyFilter);
    }

    @Override
    public ExpressionSummary getExpressionSummary(String id) throws JsonProcessingException {

        ExpressionService service = new ExpressionService();
        return service.getExpressionSummary(id);
    }

    @Override
    public String getDiseaseByExperiment(String id,
                                         int limit,
                                         int page,
                                         String sortBy,
                                         String geneticEntity,
                                         String geneticEntityType,
                                         String disease,
                                         String associationType,
                                         String evidenceCode,
                                         String source,
                                         String reference,
                                         String asc) throws JsonProcessingException {
        JsonResultResponse<DiseaseAnnotation> response = getEmpiricalDiseaseAnnotation(id,
                limit,
                page,
                sortBy,
                geneticEntity,
                geneticEntityType,
                disease,
                associationType,
                evidenceCode,
                source,
                reference,
                asc);
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        ///response.setHttpServletRequest(request);
        return mapper.writerWithView(View.DiseaseAnnotation.class).writeValueAsString(response);
    }

    @Override
    public String getDiseaseViaOrthology(String id,
                                         int limit,
                                         int page,
                                         String sortBy,
                                         String orthologyGene,
                                         String orthologyGeneSpecies,
                                         String disease,
                                         String associationType,
                                         String evidenceCode,
                                         String source,
                                         String reference,
                                         String asc) throws JsonProcessingException {
        JsonResultResponse<DiseaseAnnotation> response = getDiseaseViaOrthologyAnnotation(id,
                limit,
                page,
                sortBy,
                orthologyGene,
                orthologyGeneSpecies,
                disease,
                associationType,
                evidenceCode,
                source,
                reference,
                asc);

        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        ///response.setHttpServletRequest(request);
        return mapper.writerWithView(View.Default.class).writeValueAsString(response);
    }

    @Override
    public Response getDiseaseByExperimentDownload(String id,
                                                   String sortBy,
                                                   String geneticEntity,
                                                   String geneticEntityType,
                                                   String disease,
                                                   String associationType,
                                                   String evidenceCode,
                                                   String source,
                                                   String reference,
                                                   String asc) throws JsonProcessingException {
        JsonResultResponse<DiseaseAnnotation> response = getEmpiricalDiseaseAnnotation(id,
                null,
                null,
                sortBy,
                geneticEntity,
                geneticEntityType,
                disease,
                associationType,
                evidenceCode,
                source,
                reference,
                asc);
        Response.ResponseBuilder responseBuilder = Response.ok(diseaseTranslator.getEmpiricalDiseaseByGene(response.getResults()));
        responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
        responseBuilder.header("Content-Disposition", "attachment; filename=\"DiseaseAssociationsViaEmpiricalData-" + id.replace(":", "-") + ".tsv\"");
        return responseBuilder.build();
    }

    @Override
    public Response getDiseaseViaOrthologyDownload(String id,
                                                   String sortBy,
                                                   String orthologyGene,
                                                   String orthologyGeneSpecies,
                                                   String disease,
                                                   String associationType,
                                                   String evidenceCode,
                                                   String source,
                                                   String reference,
                                                   String asc) throws JsonProcessingException {
        JsonResultResponse<DiseaseAnnotation> response = getDiseaseViaOrthologyAnnotation(id,
                null,
                null,
                sortBy,
                orthologyGene,
                orthologyGeneSpecies,
                disease,
                associationType,
                evidenceCode,
                source,
                reference,
                asc);
        Response.ResponseBuilder responseBuilder = Response.ok(diseaseTranslator.getDiseaseViaOrthologyByGene(response.getResults()));
        responseBuilder.type(MediaType.TEXT_PLAIN_TYPE);
        responseBuilder.header("Content-Disposition", "attachment; filename=\"DiseaseAssociationsViaOrthologyData-" + id.replace(":", "-") + ".tsv\"");
        return responseBuilder.build();
    }

}
