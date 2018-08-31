package org.alliancegenome.api.rest.interfaces;


import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.alliancegenome.es.model.search.SearchResult;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;


@Path("/expression")
@Api(value = "Expression")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ExpressionRESTInterface {

    @GET
    @Path("")
    @ApiOperation(value = "Retrieve all expression records of a given set of genes")
    String getExpressionAnnotations(
            @ApiParam(name = "geneID", value = "Gene by ID", required = true)
            @QueryParam("geneID") List<String> geneIDs,
            @ApiParam(name = "filter.species", value = "filter elements", type = "String")
            @QueryParam("species") String filterSpecies,
            @ApiParam(name = "filter.gene", value = "filter elements", type = "String")
            @QueryParam("gene") String filterGene,
            @ApiParam(name = "filter.stage", value = "filter elements", type = "String")
            @QueryParam("filter.assay") String filterStage,
            @ApiParam(name = "filter.assay", value = "filter elements", type = "String")
            @QueryParam("filter.stage") String filterAssay,
            @ApiParam(name = "filter.reference", value = "filter elements", type = "String")
            @QueryParam("reference") String filterReference,
            @ApiParam(name = "filter.term", value = "filter elements", type = "String")
            @QueryParam("filter.term") String filterTerm,
            @ApiParam(name = "filter.source", value = "filter elements", type = "String")
            @QueryParam("source") String filterSource,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(name = "sortBy", value = "Sort by field name")
            @QueryParam("sortBy") String sortBy,
            @ApiParam(name = "asc", allowableValues = "true,false", value = "ascending or descending")
            @QueryParam("asc") String asc
    ) throws JsonProcessingException;

}
