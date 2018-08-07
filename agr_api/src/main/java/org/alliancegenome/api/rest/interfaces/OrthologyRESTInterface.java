package org.alliancegenome.api.rest.interfaces;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.alliancegenome.core.service.JsonResultResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/orthology")
@Api(value = "Orthology")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OrthologyRESTInterface {

    @GET
    @Path("/{species}/{species}")
    @ApiOperation(value = "Retrieve orthologous gene records for given species", notes = "Download orthology records.")
    @Produces(MediaType.TEXT_PLAIN)
    JsonResultResponse getGeneOrthology(
            @ApiParam(name = "Species One", value = "Species", required = true, type = "String")
            @PathParam("species") String speciesOne,
            @ApiParam(name = "Species Two", value = "Species", required = true, type = "String")
            @PathParam("species") String speciesTwo,
            @ApiParam(value = "stringencyFilter")
            @QueryParam("stringencyFilter") String stringencyFilter,
            @ApiParam(value = "methods")
            @QueryParam("methods") String methods,
            @ApiParam(value = "number of rows")
            @QueryParam("rows") Integer rows,
            @ApiParam(value = "start")
            @QueryParam("start") Integer start) throws IOException;

}
