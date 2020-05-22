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

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/variant")
@Api(value = "Variant Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VariantRESTInterface {

    @GET
    @Path("/{id}/transcripts")
    @ApiOperation(value = "Retrieve all transcripts of a given variant")
    @JsonView(value = {View.VariantAPI.class})
    JsonResultResponse<Transcript> getTranscriptsPerVariant(
            @ApiParam(name = "id", value = "Search for transcripts for a given variant ID")
            @PathParam("id") String id,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(value = "Field name by which to sort", allowableValues = "")
            @QueryParam("sortBy") String sortBy,
            @ApiParam(name = "filter.transcriptType", value = "Transcript types")
            @QueryParam("filter.transcriptType") String transcriptType,
            @ApiParam(name = "filter.transcriptConsequence", value = "Consequence")
            @QueryParam("filter.transcriptConsequence") String consequence
    );

}
