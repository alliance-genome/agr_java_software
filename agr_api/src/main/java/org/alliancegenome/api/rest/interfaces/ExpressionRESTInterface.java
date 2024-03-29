package org.alliancegenome.api.rest.interfaces;


import java.util.List;

import org.alliancegenome.api.dto.RibbonSummary;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/expression")
@Tag(name = "Expression")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ExpressionRESTInterface {

	@GET
	@Path("")
	@JsonView(value = {View.Expression.class})
	@Operation(summary = "Retrieve all expression records of a given set of geneMap")
	JsonResultResponse<ExpressionDetail> getExpressionAnnotations(
			@Parameter(in=ParameterIn.QUERY, name = "geneID", description = "Gene by ID", required = true)
			@QueryParam("geneID") List<String> geneIDs,
			@Parameter(in=ParameterIn.QUERY,name = "termID", description = "Term ID by which rollup should happen",schema = @Schema(type = SchemaType.STRING))
			@QueryParam("termID") String termID,
			@Parameter(in=ParameterIn.QUERY,name = "filter.species", description = "Species by taxon ID",schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.species") String filterSpecies,
			@Parameter(in=ParameterIn.QUERY,name = "filter.gene", description = "Gene symbol",schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.gene") String filterGene,
			@Parameter(in=ParameterIn.QUERY,name = "filter.stage", description = "Stage name",schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.stage") String filterStage,
			@Parameter(in=ParameterIn.QUERY,name = "filter.assay", description = "Assay name",schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.assay") String filterAssay,
			@Parameter(in=ParameterIn.QUERY,name = "filter.reference", description = "Reference",schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.reference") String filterReference,
			@Parameter(in=ParameterIn.QUERY,name = "filter.term", description = "Ontology term name",schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.term") String filterTerm,
			@Parameter(in=ParameterIn.QUERY,name = "filter.source", description = "Source",schema = @Schema(type = SchemaType.STRING))
			@QueryParam("filter.source") String filterSource,
			@Parameter(in=ParameterIn.QUERY, name="limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("20") @QueryParam("limit") Integer limit,
			@Parameter(in=ParameterIn.QUERY, name="page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("1") @QueryParam("page") Integer page,
			@Parameter(in=ParameterIn.QUERY, name="sortBy", description = "Sort by field name",schema = @Schema(type = SchemaType.STRING))// allowableValues = "Default,Species,Location,Assay,Stage,Gene")
			@DefaultValue("geneName") @QueryParam("sortBy") String sortBy,
			@Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))//,allowableValues = "true,false")
			@DefaultValue("true")
			@QueryParam("asc") String asc
	) throws JsonProcessingException;

	@GET
	@Path("/download")
	Response getExpressionAnnotationsDownload(
			@QueryParam("geneID") List<String> geneIDs,
			@QueryParam("termID") String termID,
			@QueryParam("filter.species") String filterSpecies,
			@QueryParam("filter.gene") String filterGene,
			@QueryParam("filter.stage") String filterStage,
			@QueryParam("filter.assay") String filterAssay,
			@QueryParam("filter.reference") String filterReference,
			@QueryParam("filter.term") String filterTerm,
			@QueryParam("filter.source") String filterSource,
			@QueryParam("sortBy") String sortBy,
			@QueryParam("asc") String asc
	);

	@GET
	@Path("/{taxonID}")
	@Operation(summary = "Retrieve all expression records of a given set of geneMap")
	String getExpressionAnnotationsByTaxon(
			@Parameter(in=ParameterIn.PATH,name = "taxonID", description = "Taxon ID for the first gene: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", required = true,schema = @Schema(type = SchemaType.STRING))
			@PathParam("taxonID") String speciesOne,
			@Parameter(in=ParameterIn.QUERY,name = "termID", description = "Term ID by which rollup should happen",schema = @Schema(type = SchemaType.STRING))
			@QueryParam("termID") String termID,
			@Parameter(in=ParameterIn.QUERY, name="limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("20") @QueryParam("limit") Integer limit,
			@Parameter(in=ParameterIn.QUERY, name="page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("1") @QueryParam("page") Integer page
	) throws JsonProcessingException;

	@GET
	@Path("/ribbon-summary")
	@JsonView(value = {View.Expression.class})
	@Operation(summary = "Retrieve summary of expression for given list of genes")
	RibbonSummary getExpressionSummary(
			@Parameter(in=ParameterIn.QUERY, name = "geneID", description = "list of genes for which expression data is requested", required = true)
			@QueryParam("geneID") List<String> geneIDs
	) ;

}
