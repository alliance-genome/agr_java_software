package org.alliancegenome.api.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.alliancegenome.api.rest.interfaces.ExpressionRESTInterface;
import org.alliancegenome.api.service.ExpressionService;
import org.alliancegenome.api.service.helper.ExpressionDetail;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.BioEntityGeneExpressionJoin;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ExpressionController implements ExpressionRESTInterface {

    @Context
    private HttpServletRequest request;

    private ExpressionService expressionService = new ExpressionService();
    private GeneRepository geneRepository = new GeneRepository();

    @Override
    public JsonResultResponse<ExpressionDetail> getExpressionAnnotations(List<String> geneIDs,
                                                                         String termID,
                                                                         String filterSpecies,
                                                                         String filterGene,
                                                                         String filterStage,
                                                                         String filterAssay,
                                                                         String filterReference,
                                                                         String filterTerm,
                                                                         String filterSource,
                                                                         int limit,
                                                                         int page,
                                                                         String sortBy,
                                                                         String asc) {

        LocalDateTime startDate = LocalDateTime.now();
        JsonResultResponse<ExpressionDetail> result = getExpressionDetailJsonResultResponse(
                geneIDs,
                termID,
                filterSpecies,
                filterGene,
                filterStage,
                filterAssay,
                filterReference,
                filterTerm,
                filterSource,
                limit,
                page,
                sortBy,
                asc);
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        JsonResultResponse<ExpressionDetail> response = new JsonResultResponse<>();
        response.setResults(result.getResults());
        response.calculateRequestDuration(startDate);
        response.setTotal(result.getTotal());
        response.setHttpServletRequest(request);
        return response;
    }

    private JsonResultResponse<ExpressionDetail> getExpressionDetailJsonResultResponse(List<String> geneIDs, String termID, String filterSpecies, String filterGene, String filterStage, String filterAssay, String filterReference, String filterTerm, String filterSource, int limit, int page, String sortBy, String asc) {
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        BaseFilter filterMap = new BaseFilter();
        filterMap.put(FieldFilter.FSPECIES, filterSpecies);
        filterMap.put(FieldFilter.GENE_NAME, filterGene);
        filterMap.put(FieldFilter.FREFERENCE, filterReference);
        filterMap.put(FieldFilter.SOURCE, filterSource);
        filterMap.put(FieldFilter.TERM_NAME, filterTerm);
        filterMap.put(FieldFilter.ASSAY, filterAssay);
        filterMap.put(FieldFilter.STAGE, filterStage);
        filterMap.values().removeIf(Objects::isNull);
        pagination.setFieldFilterValueMap(filterMap);

        GeneRepository geneRepository = new GeneRepository();
        List<BioEntityGeneExpressionJoin> joins = geneRepository.getExpressionAnnotations(geneIDs, termID, pagination);
        return expressionService.getExpressionDetails(joins, pagination);
    }

    @Override
    public String getExpressionAnnotationsByTaxon(String species,
                                                  String termID,
                                                  int limit,
                                                  int page) throws JsonProcessingException {
        Pagination pagination = new Pagination(page, limit, null, null);
        BaseFilter filterMap = new BaseFilter();
        filterMap.put(FieldFilter.TERM_NAME, termID);
        filterMap.values().removeIf(Objects::isNull);
        pagination.setFieldFilterValueMap(filterMap);

        LocalDateTime startDate = LocalDateTime.now();
        JsonResultResponse<ExpressionDetail> response = new JsonResultResponse<>();
        response.setHttpServletRequest(request);

        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);

        // check if valid taxon identifier
        String taxon = SpeciesType.getTaxonId(species);

        List<BioEntityGeneExpressionJoin> joins = geneRepository.getExpressionAnnotationsByTaxon(taxon, termID, pagination);

        JsonResultResponse<ExpressionDetail> result = expressionService.getExpressionDetails(joins, pagination);
        response.setResults(result.getResults());
        response.setTotal(result.getTotal());
        response.calculateRequestDuration(startDate);
        return mapper.writerWithView(View.Expression.class).writeValueAsString(response);
    }

    @Override
    public Response getExpressionAnnotationsDownload(List<String> geneIDs,
                                                     String termID,
                                                     String filterSpecies,
                                                     String filterGene,
                                                     String filterStage,
                                                     String filterAssay,
                                                     String filterReference,
                                                     String filterTerm,
                                                     String filterSource,
                                                     int limit,
                                                     int page,
                                                     String sortBy,
                                                     String asc) {

        JsonResultResponse<ExpressionDetail> result = getExpressionDetailJsonResultResponse(
                geneIDs,
                termID,
                filterSpecies,
                filterGene,
                filterStage,
                filterAssay,
                filterReference,
                filterTerm,
                filterSource,
                Integer.MAX_VALUE,
                page,
                sortBy,
                asc);

        Response.ResponseBuilder response = Response.ok((expressionService.getTextFile(result)));
        response.type(MediaType.TEXT_PLAIN_TYPE);
        String fileName = geneIDs.stream().collect(Collectors.joining("::"));
        response.header("Content-Disposition", "attachment; filename=\"expression-annotations-" + fileName + ".tsv\"");
        return response.build();
    }

}
