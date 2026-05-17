package org.alliancegenome.api.rest.interfaces;

import org.alliancegenome.core.document.LiteratureSummaryDocument;
import org.apache.commons.lang3.ObjectUtils.Null;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/reference")
@Tag(name = "Reference ")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface LiteratureRESTInterface {
	@GET
	@Path("/{id}")
	@Operation(summary = "Retrieve a literature summary object for a given id")
	@APIResponses(value = { @APIResponse(responseCode = "404", description = "Missing literature summary object", content = @Content(mediaType = "text/plain")),
		@APIResponse(responseCode = "200", description = "Literature summary object.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })

	LiteratureSummaryDocument getLiterature(@Parameter(in = ParameterIn.PATH, name = "id", description = "Search for a literature summary by ID", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id);
	
}
