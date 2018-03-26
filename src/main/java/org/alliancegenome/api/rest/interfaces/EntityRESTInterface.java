package org.alliancegenome.api.rest.interfaces;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("entity")
@Api(value = "Entity Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface EntityRESTInterface {

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Searches for any Entity", notes="Entity Notes")
    public Map<String, Object> getEntity(
            @ApiParam(name = "id", value = "Search for an entity ID")
            @PathParam("id") String id
    );
    
}
