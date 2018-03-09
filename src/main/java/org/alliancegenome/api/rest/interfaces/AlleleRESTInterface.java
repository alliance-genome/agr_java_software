package org.alliancegenome.api.rest.interfaces;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Path("/allele")
@Api(value = "Allele Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AlleleRESTInterface {

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Searches for an Allele", notes="Allele Notes")
    public Map<String, Object> getAllele(
            @ApiParam(name = "id", value = "Search for an Allele ID")
            @PathParam("id") String id
    );
    
}
