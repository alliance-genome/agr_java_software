package org.alliancegenome.api.rest.interfaces;

import java.util.Map;

import org.alliancegenome.api.response.JsonResultResponse;
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
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
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

	@GET
	@Path("/{id}/genes")
	@Operation(summary = "Retrieve distinct genes associated with the given reference")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of genes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<Map<String, Object>> getGenesByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id,
		@Parameter(in = ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("20") @QueryParam("limit") Integer limit,
		@Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("1") @QueryParam("page") Integer page);

	@GET
	@Path("/{id}/alleles")
	@Operation(summary = "Retrieve distinct alleles associated with the given reference")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of alleles", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<Map<String, Object>> getAllelesByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id,
		@Parameter(in = ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("20") @QueryParam("limit") Integer limit,
		@Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("1") @QueryParam("page") Integer page);

	@GET
	@Path("/{id}/transgenic-alleles")
	@Operation(summary = "Retrieve distinct transgenic alleles associated with the given reference")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of transgenic alleles", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<Map<String, Object>> getTransgenicAllelesByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id,
		@Parameter(in = ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("20") @QueryParam("limit") Integer limit,
		@Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("1") @QueryParam("page") Integer page);

	@GET
	@Path("/{id}/models")
	@Operation(summary = "Retrieve distinct genetic models (AGMs) associated with the given reference")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of models", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<Map<String, Object>> getModelsByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id);

}
