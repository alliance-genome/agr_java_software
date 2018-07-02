package org.alliancegenome.api.rest.interfaces;

import java.util.Map;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.alliancegenome.es.model.search.SearchResult;

@Path("/gene")
@Api(value = "Genes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GeneRESTInterface {

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Searches for a Gene", notes="Gene Notes")
    public Map<String, Object> getGene(
            @ApiParam(name = "id", value = "Search for a Gene by ID")
            @PathParam("id") String id
    );
    
    @GET
    @Path("/{id}/alleles")
    @ApiOperation(value = "Retrieve alleles of a given gene", notes="Gene Notes")
    public SearchResult getAllelesPerGene(
            @ApiParam(name = "id", value = "Search for Alleles for a given Gene by ID")
            @PathParam("id") String id
    );

    @GET
    @Path("/{id}/phenotypes")
    @ApiOperation(value = "Retrieve phenotype annotations for given gene", notes="Gene Notes")
    public SearchResult getDiseaseAnnotationsSorted(
            @ApiParam(name = "id", value = "Gene by ID", required = true, type = "String")
            @PathParam("id") String id,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(value = "Field name by which to sort", allowableValues = "phenotype,geneticEntity")
            @QueryParam("sortBy") String sortBy,
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
}
