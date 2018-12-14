package org.alliancegenome.api.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.alliancegenome.api.rest.interfaces.GeneRESTInterface;
import org.alliancegenome.api.service.ExpressionService;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.api.service.helper.ExpressionSummary;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.core.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.alliancegenome.neo4j.entity.node.BioEntityGeneExpressionJoin;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologView;
import org.alliancegenome.neo4j.view.OrthologyFilter;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RequestScoped
public class GeneController extends BaseController implements GeneRESTInterface {

    public static final String API_VERSION = "0.9";
    @Inject
    private GeneService geneService;
    private final PhenotypeAnnotationToTdfTranslator translator = new PhenotypeAnnotationToTdfTranslator();
    private ObjectMapper mapper = new ObjectMapper();

    @Context
    private HttpServletResponse response;

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Context
    private HttpServletRequest request;

    @Override
    public Map<String, Object> getGene(String id) {
        Map<String, Object> ret = geneService.getById(id);
        if (ret == null) {
            throw new NotFoundException();
        } else {
            return ret;
        }
    }

    @Override
    public SearchApiResponse getAllelesPerGene(String id) {
        return geneService.getAllelesByGene(id);
    }

    @Override
    public String getPhenotypeAnnotations(String id,
                                          int limit,
                                          int page,
                                          String sortBy,
                                          String geneticEntity,
                                          String geneticEntityType,
                                          String phenotype,
                                          String reference,
                                          String asc) throws JsonProcessingException {
        JsonResultResponse<PhenotypeAnnotation> response = getPhenotypeAnnotationDocumentJsonResultResponse(id, limit, page, sortBy, geneticEntity, geneticEntityType, phenotype, reference, asc);
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        response.setHttpServletRequest(request);
        return mapper.writerWithView(View.PhenotypeView.class).writeValueAsString(response);
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

    @Override
    public String getGeneOrthology(String id,
                                   List<String> geneIDs,
                                   String geneLister,
                                   String stringencyFilter,
                                   List<String> taxonIDs,
                                   List<String> methods,
                                   Integer rows,
                                   Integer start) throws IOException {
        LocalDateTime startDate = LocalDateTime.now();
        GeneRepository repo = new GeneRepository();
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
        List<Gene> genes = repo.getOrthologyGenes(geneList);
        OrthologyFilter orthologyFilter = new OrthologyFilter(stringencyFilter, taxonIDs, methods);
        if (rows != null && rows > 0) {
            orthologyFilter.setRows(rows);
        }
        orthologyFilter.setStart(start);
        JsonResultResponse<OrthologView> response = OrthologyService.getOrthologyMultiGeneJson(genes, orthologyFilter);
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        response.calculateRequestDuration(startDate);
        response.setApiVersion(API_VERSION);
        response.setHttpServletRequest(request);
        return mapper.writerWithView(View.OrthologyView.class).writeValueAsString(response);
    }

    @Override
    public String getInteractions(String id) {
        //return geneService.getInteractions(id);
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setSerializationInclusion(Include.NON_NULL);
        try {
            return mapper.writerWithView(View.InteractionView.class).writeValueAsString(geneService.getInteractions(id));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public String getExpressionSummary(String id) throws JsonProcessingException {

        GeneRepository geneRepository = new GeneRepository();
        List<BioEntityGeneExpressionJoin> joins = geneRepository.getExpressionAnnotationSummary(id);
        ExpressionService service = new ExpressionService();
        ExpressionSummary response = service.getExpressionSummary(joins);
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        return mapper.writerWithView(View.ExpressionView.class).writeValueAsString(response);
    }

}
