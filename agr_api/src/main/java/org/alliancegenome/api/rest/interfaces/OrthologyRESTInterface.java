package org.alliancegenome.api.rest.interfaces;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/homologs")
@Api(value = "Homology")
@Consumes(MediaType.APPLICATION_JSON)
public interface OrthologyRESTInterface {

    @GET
    @Path("/{taxonIDOne}/{taxonIDTwo}")
    @ApiOperation(value = "Retrieve homologous gene records for given species", notes = "Download orthology records.")
    String getDoubleSpeciesOrthology(
            @ApiParam(name = "taxonIDOne", value = "Taxon ID for the first gene", required = true, type = "String")
            @PathParam("taxonIDOne") String speciesOne,
            @ApiParam(name = "taxonIDTwo", value = "Taxon ID for the second gene", required = true, type = "String")
            @PathParam("taxonIDTwo") String speciesTwo,
            @ApiParam(value = "apply stringency filter", allowableValues = "stringent, moderate, all", defaultValue = "stringent")
            @QueryParam("stringencyFilter") String stringencyFilter,
            @ApiParam(value = "methods")
            @QueryParam("methods") List<String> methods,
            @ApiParam(value = "number of rows")
            @DefaultValue("20") @QueryParam("rows") Integer rows,
            @ApiParam(value = "start")
            @DefaultValue("1") @QueryParam("start") Integer start) throws IOException;

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
