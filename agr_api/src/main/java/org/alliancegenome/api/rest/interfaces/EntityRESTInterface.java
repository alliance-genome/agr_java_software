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
