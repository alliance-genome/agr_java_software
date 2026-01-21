package org.alliancegenome.api.rest.interfaces;

import java.io.IOException;
import java.util.List;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.OrthoAlgorithm;
import org.alliancegenome.neo4j.view.HomologView;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.lang3.ObjectUtils.Null;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/homologs")
@Tag(name = "Homology")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OrthologyRESTInterface {

	@GET
	@Path("/species")
	@JsonView(value = { View.Homology.class })
	@APIResponses(value = { @APIResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<HomologView> getMultiSpeciesOrthology(@QueryParam("taxonID") List<String> taxonID, @QueryParam("taxonIdList") String taxonIdList, @QueryParam("stringencyFilter") String stringencyFilter, @QueryParam("methods") String methods,
		@DefaultValue("20") @QueryParam("rows") Integer rows, @DefaultValue("1") @QueryParam("start") Integer start) throws IOException;

	@GET
	@Path("/methods")
	@JsonView(value = { View.OrthologyMethod.class })
	@Operation(summary = "Retrieve all methods used for calculation of homology")
	@APIResponses(value = { @APIResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<OrthoAlgorithm> getAllMethodsCalculations() throws JsonProcessingException;
}
