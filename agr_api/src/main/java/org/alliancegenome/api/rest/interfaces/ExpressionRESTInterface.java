package org.alliancegenome.api.rest.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.alliancegenome.api.dto.RibbonSummary;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.lang3.ObjectUtils.Null;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/expression")
@Tag(name = "Expression")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ExpressionRESTInterface {

	@POST
	@Path("")
	@Operation(summary = "Retrieve all expression records of a given set of geneMap")
	@APIResponses(value = {@APIResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class)))})
	JsonResultResponse<GeneExpressionDocument> getExpressionAnnotations(

		@Parameter(in = ParameterIn.QUERY, name = "termID", description = "Term ID by which rollup should happen", schema = @Schema(type = SchemaType.STRING))
		@QueryParam("termID") String termID,

		@Parameter(in = ParameterIn.QUERY, name = "focusTaxonId", description = "taxon ID by which rollup should happen", schema = @Schema(type = SchemaType.STRING))
		@QueryParam("focusTaxonId") String focusTaxonId,

		@Parameter(in = ParameterIn.QUERY, name = "filter.species", description = "Species by taxon ID", schema = @Schema(type = SchemaType.STRING))
		@QueryParam("filter.species") String filterSpecies,

		@Parameter(in = ParameterIn.QUERY, name = "filter.gene", description = "Gene symbol", schema = @Schema(type = SchemaType.STRING))
		@QueryParam("filter.gene") String filterGene,

		@Parameter(in = ParameterIn.QUERY, name = "filter.stage", description = "Stage name", schema = @Schema(type = SchemaType.STRING))
		@QueryParam("filter.stage") String filterStage,

		@Parameter(in = ParameterIn.QUERY, name = "filter.assay", description = "Assay name", schema = @Schema(type = SchemaType.STRING))
		@QueryParam("filter.assay") String filterAssay,

		@Parameter(in = ParameterIn.QUERY, name = "filter.reference", description = "Reference", schema = @Schema(type = SchemaType.STRING))
		@QueryParam("filter.reference") String filterReference,

		@Parameter(in = ParameterIn.QUERY, name = "filter.location", description = "Location", schema = @Schema(type = SchemaType.STRING))
		@QueryParam("filter.location") String filterLocation,

		@Parameter(in = ParameterIn.QUERY, name = "filter.source", description = "Source", schema = @Schema(type = SchemaType.STRING))
		@QueryParam("filter.source") String filterSource,

		@Parameter(in = ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
		@DefaultValue("20")
		@QueryParam("limit") Integer limit,

		@Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
		@DefaultValue("1")
		@QueryParam("page") Integer page,

		@Parameter(in = ParameterIn.QUERY, name = "sortBy", description = "Sort by field name", schema = @Schema(type = SchemaType.STRING)) // allowableValues = "Default,Species,Location,Assay,Stage,Gene")
		@DefaultValue("default")
		@QueryParam("sortBy") String sortBy,

		@Parameter(in = ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING)) // ,allowableValues = "true,false")
		@DefaultValue("true")
		@QueryParam("asc") String asc,

		//@Parameter(in = ParameterIn.QUERY, name = "geneID", description = "Gene by ID", required = true)
		@RequestBody List<String> geneIDs

	) throws JsonProcessingException;

	@POST
	@Path("/download")
	@Produces(MediaType.TEXT_PLAIN)
	Response getExpressionAnnotationsDownload(@QueryParam("termID") String termID,
											@QueryParam("focusTaxonId") String focusTaxonId,
											@QueryParam("filter.species") String filterSpecies,
											@QueryParam("filter.gene") String filterGene,
											@QueryParam("filter.stage") String filterStage,
											@QueryParam("filter.assay") String filterAssay,
											@QueryParam("filter.reference") String filterReference,
											@QueryParam("filter.location") String filterLocation,
											@QueryParam("filter.source") String filterSource,
											@QueryParam("sortBy") String sortBy,
											@QueryParam("asc") String asc,
											//@Parameter(in = ParameterIn.QUERY, name = "geneID", description = "Gene by ID", required = true)
											@RequestBody List<String> geneIDs
	);

	@GET
	@Path("/{taxonID}")
	@Operation(summary = "Retrieve all expression records of a given set of geneMap")
	String getExpressionAnnotationsByTaxon(
		@Parameter(in = ParameterIn.PATH, name = "taxonID", description = "Taxon ID for the first gene: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", required = true, schema = @Schema(type = SchemaType.STRING)) @PathParam("taxonID") String speciesOne,
		@Parameter(in = ParameterIn.QUERY, name = "termID", description = "Term ID by which rollup should happen", schema = @Schema(type = SchemaType.STRING)) @QueryParam("termID") String termID,
		@Parameter(in = ParameterIn.QUERY, name = "limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("20") @QueryParam("limit") Integer limit,
		@Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER)) @DefaultValue("1") @QueryParam("page") Integer page) throws JsonProcessingException;

	@POST
	@Path("/ribbon-summary")
	@JsonView(value = {View.Expression.class})
	@Operation(summary = "Retrieve summary of expression for given list of genes")
	@APIResponses(value = {@APIResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Null.class)))})
	RibbonSummary getExpressionSummary(
		@Parameter(in = ParameterIn.QUERY, name = "geneID", description = "list of genes for which expression data is requested", required = true)
		@RequestBody List<String> geneIDs
	);

}
