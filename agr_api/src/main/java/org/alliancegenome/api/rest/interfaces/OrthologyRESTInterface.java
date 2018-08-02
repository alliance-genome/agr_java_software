package org.alliancegenome.api.rest.interfaces;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.alliancegenome.es.model.search.SearchResult;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

@Path("/orthology")
@Api(value = "Orthology")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OrthologyRESTInterface {

    @GET
    @Path("/species")
    @ApiOperation(value = "Retrieve orthologous gene records for given species", notes = "Download orthology records.")
    @Produces(MediaType.TEXT_PLAIN)
    String getGeneOrthology(
                            @ApiParam(value = "speciesList")
                            @QueryParam("speciesList") String species,
                            @ApiParam(value = "number of rows")
                            @QueryParam("rows") Integer rows,
                            @ApiParam(value = "start")
                            @QueryParam("start") Integer start) throws IOException;

}
