package org.alliancegenome.api.rest.interfaces;

import java.util.List;
import java.util.Map;

import org.alliancegenome.api.entity.DiseaseAnnotationDocument;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.api.entity.GeneGeneticInteractionDocument;
import org.alliancegenome.api.entity.GeneMolecularInteractionDocument;
import org.alliancegenome.api.entity.LiteratureSummaryDocument;
import org.alliancegenome.api.entity.PhenotypeAnnotationDocument;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
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
	@Path("/{id}/disease-annotations")
	@Operation(summary = "Retrieve disease annotations that cite the given reference")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of disease annotations", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<DiseaseAnnotationDocument> getDiseaseAnnotationsByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie, e.g. AGRKB:101000000828456", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id,
		@Parameter(in = ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("20") @QueryParam("limit") Integer limit,
		@Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("1") @QueryParam("page") Integer page,
		@Parameter(in = ParameterIn.QUERY, name = "sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING)) @DefaultValue("") @QueryParam("sortBy") String sortBy,
		@Parameter(in = ParameterIn.QUERY, name = "filter.species", description = "filter by species", schema = @Schema(type = SchemaType.STRING)) @QueryParam("filter.species") String species,
		@Parameter(in = ParameterIn.QUERY, name = "filter.disease", description = "filter by disease name", schema = @Schema(type = SchemaType.STRING)) @QueryParam("filter.disease") String diseaseName,
		@Parameter(in = ParameterIn.QUERY, name = "filter.associationType", description = "filter by association type", schema = @Schema(type = SchemaType.STRING)) @QueryParam("filter.associationType") String associationType,
		@Parameter(in = ParameterIn.QUERY, name = "filter.evidenceCode", description = "filter by evidence code", schema = @Schema(type = SchemaType.STRING)) @QueryParam("filter.evidenceCode") String evidenceCode,
		@Parameter(in = ParameterIn.QUERY, name = "filter.dataProvider", description = "filter by source", schema = @Schema(type = SchemaType.STRING)) @QueryParam("filter.dataProvider") String dataProvider,
		@Parameter(in = ParameterIn.QUERY, name = "asc", description = "sort order", schema = @Schema(type = SchemaType.STRING)) @DefaultValue("true") @QueryParam("asc") String asc);

	@GET
	@Path("/{id}/phenotype-annotations")
	@Operation(summary = "Retrieve phenotype annotations that cite the given reference")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of phenotype annotations", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<PhenotypeAnnotationDocument> getPhenotypeAnnotationsByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id,
		@Parameter(in = ParameterIn.QUERY, name = "limit", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("20") @QueryParam("limit") Integer limit,
		@Parameter(in = ParameterIn.QUERY, name = "page", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("1") @QueryParam("page") Integer page,
		@Parameter(in = ParameterIn.QUERY, name = "sortBy", schema = @Schema(type = SchemaType.STRING)) @DefaultValue("") @QueryParam("sortBy") String sortBy,
		@Parameter(in = ParameterIn.QUERY, name = "filter.species", schema = @Schema(type = SchemaType.STRING)) @QueryParam("filter.species") String species,
		@Parameter(in = ParameterIn.QUERY, name = "filter.phenotype", schema = @Schema(type = SchemaType.STRING)) @QueryParam("filter.phenotype") String phenotype,
		@Parameter(in = ParameterIn.QUERY, name = "asc", schema = @Schema(type = SchemaType.STRING)) @DefaultValue("true") @QueryParam("asc") String asc);

	@GET
	@Path("/{id}/expression-annotations")
	@Operation(summary = "Retrieve expression annotations that cite the given reference")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of expression annotations", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<GeneExpressionDocument> getExpressionAnnotationsByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id,
		@Parameter(in = ParameterIn.QUERY, name = "crossReferences", description = "Comma-separated PMID/MOD curies for filtering (required on stage ES)", schema = @Schema(type = SchemaType.STRING)) @QueryParam("crossReferences") List<String> crossReferences,
		@Parameter(in = ParameterIn.QUERY, name = "limit", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("20") @QueryParam("limit") Integer limit,
		@Parameter(in = ParameterIn.QUERY, name = "page", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("1") @QueryParam("page") Integer page,
		@Parameter(in = ParameterIn.QUERY, name = "sortBy", schema = @Schema(type = SchemaType.STRING)) @DefaultValue("") @QueryParam("sortBy") String sortBy,
		@Parameter(in = ParameterIn.QUERY, name = "filter.species", schema = @Schema(type = SchemaType.STRING)) @QueryParam("filter.species") String species,
		@Parameter(in = ParameterIn.QUERY, name = "asc", schema = @Schema(type = SchemaType.STRING)) @DefaultValue("true") @QueryParam("asc") String asc);

	@GET
	@Path("/{id}/molecular-interactions")
	@Operation(summary = "Retrieve molecular interactions that cite the given reference")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of molecular interactions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<GeneMolecularInteractionDocument> getMolecularInteractionsByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id,
		@Parameter(in = ParameterIn.QUERY, name = "crossReferences", description = "Comma-separated PMID/MOD curies for filtering (required on stage ES)", schema = @Schema(type = SchemaType.STRING)) @QueryParam("crossReferences") List<String> crossReferences,
		@Parameter(in = ParameterIn.QUERY, name = "limit", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("20") @QueryParam("limit") Integer limit,
		@Parameter(in = ParameterIn.QUERY, name = "page", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("1") @QueryParam("page") Integer page,
		@Parameter(in = ParameterIn.QUERY, name = "sortBy", schema = @Schema(type = SchemaType.STRING)) @DefaultValue("") @QueryParam("sortBy") String sortBy,
		@Parameter(in = ParameterIn.QUERY, name = "filter.species", schema = @Schema(type = SchemaType.STRING)) @QueryParam("filter.species") String species,
		@Parameter(in = ParameterIn.QUERY, name = "asc", schema = @Schema(type = SchemaType.STRING)) @DefaultValue("true") @QueryParam("asc") String asc);

	@GET
	@Path("/{id}/genetic-interactions")
	@Operation(summary = "Retrieve genetic interactions that cite the given reference")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of genetic interactions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<GeneGeneticInteractionDocument> getGeneticInteractionsByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id,
		@Parameter(in = ParameterIn.QUERY, name = "crossReferences", description = "Comma-separated PMID/MOD curies (required on stage ES)", schema = @Schema(type = SchemaType.STRING)) @QueryParam("crossReferences") List<String> crossReferences,
		@Parameter(in = ParameterIn.QUERY, name = "limit", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("20") @QueryParam("limit") Integer limit,
		@Parameter(in = ParameterIn.QUERY, name = "page", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("1") @QueryParam("page") Integer page,
		@Parameter(in = ParameterIn.QUERY, name = "sortBy", schema = @Schema(type = SchemaType.STRING)) @DefaultValue("") @QueryParam("sortBy") String sortBy,
		@Parameter(in = ParameterIn.QUERY, name = "filter.species", schema = @Schema(type = SchemaType.STRING)) @QueryParam("filter.species") String species,
		@Parameter(in = ParameterIn.QUERY, name = "asc", schema = @Schema(type = SchemaType.STRING)) @DefaultValue("true") @QueryParam("asc") String asc);

	@GET
	@Path("/{id}/genes")
	@Operation(summary = "Retrieve distinct genes associated with the given reference")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of genes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<Map<String, Object>> getGenesByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id);

	@GET
	@Path("/{id}/alleles")
	@Operation(summary = "Retrieve distinct alleles associated with the given reference")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of alleles", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<Map<String, Object>> getAllelesByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id);

	@GET
	@Path("/{id}/related-papers")
	@Operation(summary = "Papers sharing gene subjects with this reference, ranked by Jaccard similarity")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of related papers", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<Map<String, Object>> getRelatedPapersByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id,
		@Parameter(in = ParameterIn.QUERY, name = "limit", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("10") @QueryParam("limit") Integer limit,
		@Parameter(in = ParameterIn.QUERY, name = "includeOrthologs", description = "Expand gene set via orthology to include cross-species papers", schema = @Schema(type = SchemaType.BOOLEAN)) @DefaultValue("false") @QueryParam("includeOrthologs") Boolean includeOrthologs);

	@GET
	@Path("/{id}/orthology")
	@Operation(summary = "Retrieve orthologs of genes associated with the given reference")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of orthology pairs", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<Map<String, Object>> getOrthologyByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id,
		@Parameter(in = ParameterIn.QUERY, name = "limit", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("20") @QueryParam("limit") Integer limit,
		@Parameter(in = ParameterIn.QUERY, name = "page", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("1") @QueryParam("page") Integer page,
		@Parameter(in = ParameterIn.QUERY, name = "sortBy", schema = @Schema(type = SchemaType.STRING)) @DefaultValue("") @QueryParam("sortBy") String sortBy,
		@Parameter(in = ParameterIn.QUERY, name = "asc", schema = @Schema(type = SchemaType.STRING)) @DefaultValue("true") @QueryParam("asc") String asc);

	@GET
	@Path("/{id}/models")
	@Operation(summary = "Retrieve distinct genetic models (AGMs) associated with the given reference")
	@APIResponses(value = { @APIResponse(responseCode = "200", description = "List of models", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class))) })
	JsonResultResponse<Map<String, Object>> getModelsByReference(
		@Parameter(in = ParameterIn.PATH, name = "id", description = "Reference curie", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("id") String id);

}
