package org.alliancegenome.api.rest.interfaces;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;


@Path("/geneMap")
@Api(value = "Genes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GenesRESTInterface {

    @GET
    @Path("/")
    @ApiOperation(value = "Retrieve gene records")
    String getGenes(
            @ApiParam(name = "taxonID", value = "Species identifier: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", type = "String")
            @QueryParam("taxonID") List<String> taxonID,
            @ApiParam(value = "maximum number of rows returned")
            @DefaultValue("20") @QueryParam("rows") Integer rows,
            @ApiParam(value = "starting row number (for pagination)")
            @DefaultValue("1") @QueryParam("start") Integer start) throws IOException;


    @GET
    @Path("/geneIDs")
    @ApiOperation(value = "Retrieve list of gene IDs")
    @Produces(MediaType.TEXT_PLAIN)
    String getGeneIDs(
            @ApiParam(name = "taxonID", value = "Species identifier: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", type = "String")
            @QueryParam("taxonID") List<String> taxonID,
            @ApiParam(value = "maximum number of rows returned")
            @DefaultValue("20") @QueryParam("rows") Integer rows,
            @ApiParam(value = "starting row number (for pagination)")
            @DefaultValue("1") @QueryParam("start") Integer start) throws IOException;


}
