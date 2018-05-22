package org.alliancegenome.api.rest.interfaces;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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

}
