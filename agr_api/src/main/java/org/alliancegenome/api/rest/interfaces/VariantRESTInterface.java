package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.*;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.annotation.JsonView;

@Path("/variant")
@Tag(name = "Variant Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VariantRESTInterface {

    @GET
    @Path("/{id}/transcripts")
    @Operation(summary = "Retrieve all transcripts of a given variant")
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = "404",
                            description = "Missing description",
                            content = @Content(mediaType = "text/plain")),
                    @APIResponse(
                            responseCode = "200",
                            description = "JVM system properties of a particular host.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Transcript.class))) })
    @JsonView(value = {View.VariantAPI.class})
    JsonResultResponse<Transcript> getTranscriptsPerVariant(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Search for transcripts for a given variant ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") int limit,
            @Parameter(in=ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") int page,
            @Parameter(in=ParameterIn.QUERY, name = "sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("symbol") @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name = "filter.transcriptType", description = "Transcript types", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.transcriptType") String transcriptType,
            @Parameter(in=ParameterIn.QUERY, name = "filter.transcriptConsequence", description = "Consequence", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.transcriptConsequence") String consequence
    );

}
