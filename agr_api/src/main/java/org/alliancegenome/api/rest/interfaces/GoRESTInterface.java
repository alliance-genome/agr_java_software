package org.alliancegenome.api.rest.interfaces;

import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/go")
//@Api(value = "Go Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GoRESTInterface {

	@GET
	@Path("/{id}")
	@Operation(summary = "Searches for a Go fields", hidden = true)

	@APIResponses(
			value = {
					@APIResponse(
							responseCode = "404",
							description = "Missing GO",
							content = @Content(mediaType = "text/plain")),
					@APIResponse(
							responseCode = "200",
							description = "GOTerm.",
							content = @Content(mediaType = "application/json",
									schema = @Schema(implementation = GOTerm.class))) })
	public GOTerm getGo(
			@Parameter(in=ParameterIn.PATH, name = "id", description = "Search for a Go Term by ID", required=true, schema = @Schema(type = SchemaType.STRING))
			@PathParam("id") String id
	);
	
}
