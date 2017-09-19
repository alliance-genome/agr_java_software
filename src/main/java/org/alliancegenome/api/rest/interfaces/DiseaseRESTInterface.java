package org.alliancegenome.api.rest.interfaces;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.alliancegenome.api.model.SearchResult;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/disease")
@Api(value = "Disease Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DiseaseRESTInterface {

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Searches for a Disease fields", notes = "Disease Notes")
    public Map<String, Object> getDisease(
            @ApiParam(name = "id", value = "Search for Disease ID")
            @PathParam("id") String id
    );

    @GET
    @Path("/{id}/associations")
    @ApiOperation(value = "Searches for a Disease fields", notes = "Disease Notes")
    public SearchResult getDiseaseAnnotations(
            @ApiParam(name = "id", value = "Search for Disease ID")
            @PathParam("id") String id,

            @DefaultValue("20") @QueryParam("limit") int limit,

            @ApiParam(name = "offset", value = "This specifies which (page size) to request for")
            @DefaultValue("0") @QueryParam("offset") int offset);

}
