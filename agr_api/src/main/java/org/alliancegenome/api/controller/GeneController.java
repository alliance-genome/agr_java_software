package org.alliancegenome.api.controller;

import io.swagger.annotations.ApiParam;
import org.alliancegenome.api.rest.interfaces.GeneRESTInterface;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.core.service.OrthologyService;
import org.alliancegenome.core.translators.tdf.PhenotypeAnnotationToTdfTranslator;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchResult;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.OrthologyFilter;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

@RequestScoped
public class GeneController extends BaseController implements GeneRESTInterface {

    @Inject
    private GeneService geneService;
    private final PhenotypeAnnotationToTdfTranslator translator = new PhenotypeAnnotationToTdfTranslator();

    @Context
    private HttpServletResponse response;

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
    public SearchResult getAllelesPerGene(String id) {
        return geneService.getAllelesByGene(id);
    }

    @Override
    public SearchResult getPhenotypeAnnotations(String id,
                                                int limit,
                                                int page,
                                                String sortBy,
                                                String geneticEntity,
                                                String geneticEntityType,
                                                String phenotype,
                                                String reference,
                                                String asc) {
        if (sortBy.isEmpty())
            sortBy = FieldFilter.PHENOTYPE.getName();
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY, geneticEntity);
        pagination.addFieldFilter(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityType);
        pagination.addFieldFilter(FieldFilter.PHENOTYPE, phenotype);
        pagination.addFieldFilter(FieldFilter.REFERENCE, reference);
        return getSearchResult(id, pagination);
    }

    private SearchResult getSearchResult(String id, Pagination pagination) {
        if (pagination.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            try {
                response.flushBuffer();
            } catch (Exception ignored) {
            }
            SearchResult searchResult = new SearchResult();
            searchResult.errorMessages = pagination.getErrorList();
            return searchResult;
        }
        return geneService.getPhenotypeAnnotations(id, pagination);
    }

    @Override
    public Response getPhenotypeAnnotationsDownloadFile(String id) {

        Response.ResponseBuilder response = Response.ok(getPhenotypeAnnotationsDownload(id));
        response.type(MediaType.TEXT_PLAIN_TYPE);
        response.header("Content-Disposition", "attachment; filename=\"phenotype-annotations-" + id.replace(":", "-") + ".tsv\"");
        return response.build();
    }

    @Override
    public String getGeneOrthology(@ApiParam(name = "id", value = "Gene ID", required = true, type = "String")
                                           String id,
                                   @ApiParam(value = "apply stringency filter", allowableValues = "all, moderate, stringent", defaultValue = "all")
                                           String stringencyFilter,
                                   @ApiParam(value = "list of species")
                                           String species,
                                   @ApiParam(value = "list of methods")
                                           String methods,
                                   @ApiParam(value = "rows", required = false)
                                           Integer rows,
                                   @ApiParam(value = "start row", required = false, defaultValue = "0")
                                           Integer start) throws IOException {
        GeneRepository repo = new GeneRepository();
        Gene gene = repo.getOrthologyGene(id);
        OrthologyFilter orthologyFilter = new OrthologyFilter(stringencyFilter, species, methods);
        return OrthologyService.getOrthologyJson(gene, orthologyFilter);
    }

    public String getPhenotypeAnnotationsDownload(String id) {
        Pagination pagination = new Pagination(1, Integer.MAX_VALUE, "phenotype", null);
        // retrieve all records
        return translator.getAllRows(geneService.getPhenotypeAnnotationsDownload(id, pagination));
    }

}
