package org.alliancegenome.api.rest.interfaces;

import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
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

@Path("/variant")
@Tag(name = "Variant Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VariantRESTInterface {

	@GET
	@Path("/{id}")
	@Operation(description = "Searches for a Variant", summary = "Variant by ID")
	@APIResponses(value = { @APIResponse(responseCode = "404", description = "Missing variant", content = @Content(mediaType = "text/plain")),
		@APIResponse(responseCode = "200", description = "Search for Variant.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	VariantSummaryDocument getVariant(@Parameter(in = ParameterIn.PATH, name = "id", description = "Search for a Variant by ID", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id);

}
