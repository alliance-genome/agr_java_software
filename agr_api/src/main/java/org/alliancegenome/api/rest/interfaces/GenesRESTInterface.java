package org.alliancegenome.api.rest.interfaces;


import java.io.IOException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.annotation.JsonView;


@Path("/geneMap")
@Tag(name = "Genes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GenesRESTInterface {

    @GET
    @Path("/")
    @Operation(summary = "Retrieve gene records")
    @JsonView({View.Orthology.class})
    JsonResultResponse<Gene> getGenes(
            //@ApiParam(name = "taxonID", value = "Species identifier: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", type = "String")
            @QueryParam("taxonID") List<String> taxonID,
            //@ApiParam(value = "maximum number of rows returned")
            @DefaultValue("20") @QueryParam("rows") Integer rows,
            //@ApiParam(value = "starting row number (for pagination)")
            @DefaultValue("1") @QueryParam("start") Integer start) throws IOException;


    @GET
    @Path("/geneIDs")
    @Operation(summary = "Retrieve list of gene IDs")
    @Produces(MediaType.TEXT_PLAIN)
    String getGeneIDs(
            //@ApiParam(name = "taxonID", value = "Species identifier: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", type = "String")
            @QueryParam("taxonID") List<String> taxonID,
            //@ApiParam(value = "maximum number of rows returned")
            @DefaultValue("20") @QueryParam("rows") Integer rows,
            //@ApiParam(value = "starting row number (for pagination)")
            @DefaultValue("1") @QueryParam("start") Integer start) throws IOException;


}
