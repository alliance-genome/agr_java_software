package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.entity.*;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.*;
import org.eclipse.microprofile.openapi.annotations.media.*;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.*;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.annotation.JsonView;

@Path("/allele")
@Tag(name = "Allele Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AlleleRESTInterface {

    @GET
    @Path("/{id}")
    @JsonView({View.AlleleAPI.class})
    @Operation(description = "Searches for an Allele", summary = "Allele Notes")
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = "404",
                            description = "Missing alleles",
                            content = @Content(mediaType = "text/plain")),
                    @APIResponse(
                            responseCode = "200",
                            description = "Search for alleles.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Allele.class))) })
    public Allele getAllele(
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Search for an Allele by ID", required = true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id
            );

    @GET
    @Path("/{id}/variants")
    @Operation(summary = "Retrieve all variants of a given allele", description="Retrieve all variants of a given allele")
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = "404",
                            description = "Missing variants",
                            content = @Content(mediaType = "text/plain")),
                    @APIResponse(
                            responseCode = "200",
                            description = "Variants for a allele.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Variant.class))) })
    @JsonView(value = {View.VariantAPI.class})
    JsonResultResponse<Variant> getVariantsPerAllele(
            @Parameter(in=ParameterIn.PATH, name="id", description = "Search for Variants for a given Allele by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name="limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") Integer limit,
            @Parameter(in=ParameterIn.QUERY, name="page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") Integer page,
            @Parameter(in=ParameterIn.QUERY, name="sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name="filter.variantType", description = "Variant types", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.variantType") String variantType,
            @Parameter(in=ParameterIn.QUERY, name="filter.molecularConsequence", description = "Consequence", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.molecularConsequence") String molecularConsequence
            );

    @GET
    @Path("/{id}/variants/download")
    @Operation(summary = "Retrieve all variants of a given allele in a download")
    @JsonView(value = {View.VariantAPI.class})
    @Produces(MediaType.TEXT_PLAIN)
    Response getVariantsPerAlleleDownload(
            @Parameter(in=ParameterIn.PATH, name="id", description = "Search for Variants for a given Allele by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name="sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("sortBy") String sortBy,
            @Parameter(in=ParameterIn.QUERY, name="filter.variantType", description = "Variant types", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.variantType") String variantType,
            @Parameter(in=ParameterIn.QUERY, name="filter.variantConsequence", description = "Consequence", schema = @Schema(type = SchemaType.STRING))
            @QueryParam("filter.variantConsequence") String consequence
            );

    @GET
    @Path("/species/{species}")
    @Operation(summary = "Retrieve all alleles of a given species")
    @JsonView(value = {View.GeneAllelesAPI.class})
    JsonResultResponse<Allele> getAllelesPerSpecies(
            @Parameter(
                    in=ParameterIn.PATH, 
                    name="species", 
                    description = "Species identifier: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", 
                    required=true, 
                    schema = @Schema(type = SchemaType.STRING))
            @PathParam("species") String species,
            @Parameter(in=ParameterIn.QUERY, name="limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") Integer limit,
            @Parameter(in=ParameterIn.QUERY, name="page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") Integer page,
            @Parameter(in=ParameterIn.QUERY, name="sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("symbol") @QueryParam("sortBy") String sortBy, // allowedValues = "symbol,name"
            @Parameter(in=ParameterIn.QUERY, name = "asc", description = "order to sort by", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("true") @QueryParam("asc") String asc
            );

    @GET
    @Path("/{id}/phenotypes")
    @Operation(summary = "Retrieve all phenotypes of a given allele")
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = "404",
                            description = "Missing phenotypes",
                            content = @Content(mediaType = "text/plain")),
                    @APIResponse(
                            responseCode = "200",
                            description = "Phenotypes for a given Allele.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = PhenotypeAnnotation.class))) })
    @JsonView(value = {View.PhenotypeAPI.class})
    JsonResultResponse<PhenotypeAnnotation> getPhenotypePerAllele(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Search for Phenotypes for a given Allele by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name="limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") Integer limit,
            @Parameter(in=ParameterIn.QUERY, name="page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") Integer page,
            @Parameter(in=ParameterIn.QUERY, name = "filter.termName", description = "termName annotation")
            @QueryParam("filter.termName") String phenotype,
            @Parameter(in=ParameterIn.QUERY, name = "filter.source", description =  "Source")
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name = "filter.reference", description = "Reference number: PUBMED or a Pub ID from the MOD")
            @QueryParam("filter.reference") String reference,
            @Parameter(in=ParameterIn.QUERY, name="sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("symbol") @QueryParam("sortBy") String sortBy

            );

    @GET
    @Path("/{id}/phenotypes/download")
    @Operation(summary = "Retrieve all phenotypes of a given allele in a download")
    @JsonView(value = {View.PhenotypeAPI.class})
    @Produces(MediaType.TEXT_PLAIN)
    Response getPhenotypesPerAlleleDownload(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Search for Phenotypes for a given Allele by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "filter.termName", description = "termName annotation")
            @QueryParam("filter.termName") String phenotype,
            @Parameter(in=ParameterIn.QUERY, name = "filter.source", description =  "Source")
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name = "filter.reference", description  = "Reference number: PUBMED or a Pub ID from the MOD")
            @QueryParam("filter.reference") String reference,
            @Parameter(in=ParameterIn.QUERY, name="sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("symbol") @QueryParam("sortBy") String sortBy
            );

    @GET
    @Path("/{id}/diseases")
    @Operation(summary = "Retrieve all diseases of a given allele")
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = "404",
                            description = "Missing diseases",
                            content = @Content(mediaType = "text/plain")),
                    @APIResponse(
                            responseCode = "200",
                            description = "Diseases for a given Allele.",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = DiseaseAnnotation.class))) })
    @JsonView(value = {View.DiseaseAnnotationSummary.class})
    JsonResultResponse<DiseaseAnnotation> getDiseasePerAllele(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Search for Phenotypes for a given Allele by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name="limit", description = "Number of rows returned", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("20") @QueryParam("limit") Integer limit,
            @Parameter(in=ParameterIn.QUERY, name="page", description = "Page number", schema = @Schema(type = SchemaType.INTEGER))
            @DefaultValue("1") @QueryParam("page") Integer page,
            @Parameter(in=ParameterIn.QUERY, name = "filter.disease", description = "termName annotation")
            @QueryParam("filter.disease") String disease,
            @Parameter(in=ParameterIn.QUERY, name = "filter.source", description = "Source")
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name = "filter.reference", description = "Reference number: PUBMED or a Pub ID from the MOD")
            @QueryParam("filter.reference") String reference,
            @Parameter(in=ParameterIn.QUERY, name = "filter.associationType", description = "Filter on association Type")
            @QueryParam("filter.associationType") String associationType,
            @Parameter(in=ParameterIn.QUERY, name="sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("symbol") @QueryParam("sortBy") String sortBy

            );


    @GET
    @Path("/{id}/diseases/download")
    @Operation(summary = "Retrieve all diseases of a given allele in a download")
    @JsonView(value = {View.DiseaseAnnotationSummary.class})
    @Produces(MediaType.TEXT_PLAIN)
    Response getDiseasePerAlleleDownload(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Search for Phenotypes for a given Allele by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id,
            @Parameter(in=ParameterIn.QUERY, name = "filter.disease", description = "termName annotation")
            @QueryParam("filter.disease") String disease,
            @Parameter(in=ParameterIn.QUERY, name = "filter.source", description = "Source")
            @QueryParam("filter.source") String source,
            @Parameter(in=ParameterIn.QUERY, name = "filter.reference", description = "Reference number: PUBMED or a Pub ID from the MOD")
            @QueryParam("filter.reference") String reference,
            @Parameter(in=ParameterIn.QUERY, name = "filter.associationType", description = "Filter on association Type")
            @QueryParam("filter.associationType") String associationType,
            @Parameter(in=ParameterIn.QUERY, name="sortBy", description = "Field name by which to sort", schema = @Schema(type = SchemaType.STRING))
            @DefaultValue("symbol") @QueryParam("sortBy") String sortBy
            );

}
