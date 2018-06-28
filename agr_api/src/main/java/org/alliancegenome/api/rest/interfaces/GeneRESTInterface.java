package org.alliancegenome.api.rest.interfaces;

import java.util.Map;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.alliancegenome.es.model.search.SearchResult;

@Path("/gene")
@Api(value = "Gene Search")
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
    @ApiOperation(value = "Searches for all Alleles of a given Gene", notes="Gene Notes")
    public SearchResult getAllelesPerGene(
            @ApiParam(name = "id", value = "Search for Alleles for a given Gene by ID")
            @PathParam("id") String id
    );

    @GET
    @Path("/{id}/phenotypes")
    public SearchResult getDiseaseAnnotationsSorted(
            @PathParam("id") String id,
            @DefaultValue("20") @QueryParam("limit") int limit,
            @DefaultValue("1") @QueryParam("page") int page,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("geneticEntity") String geneticEntity,
            @QueryParam("geneticEntityType") String geneticEntityType,
            @QueryParam("phenotype") String phenotype,
            @QueryParam("reference") String reference,
            @QueryParam("evidenceCode") String evidenceCode,
            @QueryParam("asc") String asc);
}
