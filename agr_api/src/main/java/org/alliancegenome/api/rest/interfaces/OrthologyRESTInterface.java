package org.alliancegenome.api.rest.interfaces;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/orthology")
@Api(value = "Orthology")
@Consumes(MediaType.APPLICATION_JSON)
public interface OrthologyRESTInterface {

    @GET
    @Path("/{speciesOne}/{speciesTwo}")
    @ApiOperation(value = "Retrieve orthologous gene records for given species", notes = "Download orthology records.")
    String getDoubleSpeciesOrthology(
            @ApiParam(name = "speciesOne", value = "Species One", required = true, type = "String")
            @PathParam("speciesOne") String speciesOne,
            @ApiParam(name = "speciesTwo", value = "Species Two", required = true, type = "String")
            @PathParam("speciesTwo") String speciesTwo,
            @ApiParam(value = "stringencyFilter")
            @QueryParam("stringencyFilter") String stringencyFilter,
            @ApiParam(value = "methods")
            @QueryParam("methods") List<String> methods,
            @ApiParam(value = "number of rows")
            @QueryParam("rows") Integer rows,
            @ApiParam(value = "start")
            @QueryParam("start") Integer start) throws IOException;

    @GET
    @Path("/{species}")
    @ApiOperation(value = "Retrieve orthologous gene records for given species", notes = "Download orthology records.")
    String getSingleSpeciesOrthology(
            @ApiParam(name = "species", value = "Species", required = true, type = "String")
            @PathParam("species") String species,
            @ApiParam(value = "stringencyFilter")
            @QueryParam("stringencyFilter") String stringencyFilter,
            @ApiParam(value = "methods")
            @QueryParam("methods") List<String> methods,
            @ApiParam(value = "number of rows")
            @QueryParam("rows") Integer rows,
            @ApiParam(value = "start")
            @QueryParam("start") Integer start) throws IOException;
}
