package org.alliancegenome.api.rest.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Transcript;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.view.View;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
