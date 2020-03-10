package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.neo4j.entity.node.GOTerm;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/go")
@Api(value = "Go Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GoRESTInterface {

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Searches for a Go fields", notes="Go Notes", hidden = true)
    public GOTerm getGo(
            @ApiParam(name = "id", value = "Search for a Go Term by ID")
            @PathParam("id") String id
    );
    
}
