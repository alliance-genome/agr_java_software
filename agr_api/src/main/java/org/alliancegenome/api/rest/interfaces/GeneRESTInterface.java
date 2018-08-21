package org.alliancegenome.api.rest.interfaces;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.search.SearchResult;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

@Path("/gene")
@Api(value = "Genes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GeneRESTInterface {

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Retrieve a Gene for given ID")
    Map<String, Object> getGene(
            @ApiParam(name = "id", value = "Gene by ID")
            @PathParam("id") String id
    );

    @GET
    @Path("/{id}/alleles")
    @ApiOperation(value = "Retrieve all alleles of a given gene")
    SearchResult getAllelesPerGene(
            @ApiParam(name = "id", value = "Search for Alleles for a given Gene by ID")
            @PathParam("id") String id
    );

    @GET
    @Path("/{id}/phenotypes")
    @ApiOperation(value = "Retrieve phenotype annotations for given gene")
    SearchResult getPhenotypeAnnotations(
            @ApiParam(name = "id", value = "Gene by ID", required = true, type = "String")
            @PathParam("id") String id,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(value = "Field name by which to sort", allowableValues = "phenotype,geneticEntity")
            @DefaultValue("phenotype") @QueryParam("sortBy") String sortBy,
            @ApiParam(name = "geneticEntity", value = "genetic entity symbol")
            @QueryParam("geneticEntity") String geneticEntity,
            @ApiParam(name = "geneticEntityType", value = "genetic entity type", allowableValues = "allele")
            @QueryParam("geneticEntityType") String geneticEntityType,
            @ApiParam(value = "phenotype annotation")
            @QueryParam("phenotype") String phenotype,
            @ApiParam(value = "Reference number: PUBMED or a Pub ID from the MOD")
            @QueryParam("reference") String reference,
            @ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc);

    @GET
    @Path("/{id}/phenotypes/download")
    @ApiOperation(value = "Retrieve all phenotype annotations for a given gene", notes = "Download all phenotype annotations for a given gene")
    @Produces(MediaType.TEXT_PLAIN)
    Response getPhenotypeAnnotationsDownloadFile(@ApiParam(name = "id", value = "Gene by ID", required = true, type = "String")
                                                 @PathParam("id") String id);

    @GET
    @Path("/{id}/orthology")
    @ApiOperation(value = "Retrieve orthologous gene records", notes = "Download orthology records.")
    String getGeneOrthology(@ApiParam(name = "id", value = "Gene ID", required = true, type = "String")
                            @PathParam("id") String id,
                                        @ApiParam(value = "apply filter", allowableValues = "all, moderate, stringent", defaultValue = "all")
                            @QueryParam("filter") String filter,
                                        @ApiParam(value = "species")
                            @QueryParam("species") String species,
                                        @ApiParam(value = "methods")
                            @QueryParam("methods") String methods,
                                        @ApiParam(value = "number of rows")
                            @DefaultValue("20") @QueryParam("rows") Integer rows,
                                        @ApiParam(value = "start")
                            @DefaultValue("0") @QueryParam("start") Integer start) throws IOException;

}
