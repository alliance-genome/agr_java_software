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

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
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

            @Parameter(in=ParameterIn.PATH, name = "taxonID", description = "Species identifier: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'", required=true, schema = @Schema(type = SchemaType.STRING))
            @QueryParam("taxonID") List<String> taxonID,
            @Parameter(in=ParameterIn.QUERY, name = "rows", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("rows") Integer rows,
            @Parameter(in=ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("start") Integer start) throws IOException;


    @GET
    @Path("/geneIDs")
    @Operation(summary = "Retrieve list of gene IDs")
    @Produces(MediaType.TEXT_PLAIN)
    String getGeneIDs(
            @Parameter(in=ParameterIn.PATH, name = "taxonID", description = "Species identifier: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'", required=true, schema = @Schema(type = SchemaType.STRING))
            @QueryParam("taxonID") List<String> taxonID,
            @Parameter(in=ParameterIn.QUERY, name = "rows", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("rows") Integer rows,
            @Parameter(in=ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("start") Integer start) throws IOException;


}
