package org.alliancegenome.api.rest.interfaces;


import java.io.IOException;
import java.util.List;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path("/geneMap")
@Tag(name = "Genes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GenesRESTInterface {

	@GET
	@Path("/")
	@Operation(summary = "Retrieve gene records")
	@JsonView({View.Homology.class})
	JsonResultResponse<Gene> getGenes(
			//@ApiParam(name = "taxonID", value = "Species identifier: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", type = "String")

			@Parameter(in = ParameterIn.QUERY, name = "taxonID", description = "Species identifier: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'", required = true, schema = @Schema(type = SchemaType.STRING))
			@QueryParam("taxonID") List<String> taxonID,
			@Parameter(in = ParameterIn.QUERY, name = "rows", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("20") @QueryParam("rows") Integer rows,
			@Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("1") @QueryParam("start") Integer start) throws IOException;


	@GET
	@Path("/geneIDs")
	@Operation(summary = "Retrieve list of gene IDs")
	@Produces(MediaType.TEXT_PLAIN)
	String getGeneIDs(
			@Parameter(in = ParameterIn.QUERY, name = "taxonID", description = "Species identifier: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'", required = true, schema = @Schema(type = SchemaType.STRING))
			@QueryParam("taxonID") List<String> taxonID,
			@Parameter(in = ParameterIn.QUERY, name = "rows", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("20") @QueryParam("rows") Integer rows,
			@Parameter(in = ParameterIn.QUERY, name = "page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
			@DefaultValue("1") @QueryParam("start") Integer start) throws IOException;


	@GET
	@Path("/species")
	@Operation(summary = "Retrieve map of MOD / NCBI / ENSEMBL genes grouped by MOD ID")
	@Produces(MediaType.TEXT_PLAIN)
	Response getIdMap(
			@Parameter(in = ParameterIn.QUERY, name = "species", description = "gene species")
			@QueryParam("species") List<String> species
	);

	@GET
	@Path("/ensembl")
	@Operation(summary = "Retrieve map of MOD / NCBI / ENSEMBL genes grouped by Ensembl ID")
	@Produces(MediaType.TEXT_PLAIN)
	Response getIdMapEnsembl(
			@Parameter(in = ParameterIn.QUERY, name = "species", description = "gene species")
			@QueryParam("species") List<String> species
	);

	@GET
	@Path("/ncbi")
	@Operation(summary = "Retrieve map of MOD / NCBI / ENSEMBL genes grouped by NCBI ID")
	@Produces(MediaType.TEXT_PLAIN)
	Response getIdMapNcbi(
			@Parameter(in = ParameterIn.QUERY, name = "species", description = "gene species")
			@QueryParam("species") List<String> species
	);

}
