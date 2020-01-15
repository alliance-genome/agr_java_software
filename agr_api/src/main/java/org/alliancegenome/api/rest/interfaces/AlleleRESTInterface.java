package org.alliancegenome.api.rest.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.view.View;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/allele")
@Api(value = "Allele Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AlleleRESTInterface {

    @GET
    @Path("/{id}")
    @JsonView({View.AlleleAPI.class})
    @ApiOperation(value = "Searches for an Allele", notes = "Allele Notes")
    public Allele getAllele(
            @ApiParam(name = "id", value = "Search for an Allele by ID")
            @PathParam("id") String id
    );

    @GET
    @Path("/{id}/variants")
    @ApiOperation(value = "Retrieve all variants of a given allele")
    @JsonView(value = {View.VariantAPI.class})
    JsonResultResponse<Variant> getVariantsPerAllele(
            @ApiParam(name = "id", value = "Search for Variants for a given Allele by ID")
            @PathParam("id") String id,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(value = "Field name by which to sort", allowableValues = "")
            @QueryParam("sortBy") String sortBy,
            @ApiParam(name = "filter.variantType", value = "Variant types")
            @QueryParam("filter.variantType") String variantType,
            @ApiParam(name = "filter.variantConsequence", value = "Consequence")
            @QueryParam("filter.variantConsequence") String consequence
    );

    @GET
    @Path("/{id}/variants/download")
    @ApiOperation(value = "Retrieve all variants of a given allele in a download")
    @JsonView(value = {View.VariantAPI.class})
    @Produces(MediaType.TEXT_PLAIN)
    Response getVariantsPerAlleleDownload(
            @ApiParam(name = "id", value = "Search for Variants for a given Allele by ID")
            @PathParam("id") String id,
            @ApiParam(value = "Field name by which to sort", allowableValues = "")
            @QueryParam("sortBy") String sortBy,
            @ApiParam(name = "filter.variantType", value = "Variant types")
            @QueryParam("filter.variantType") String variantType,
            @ApiParam(name = "filter.variantConsequence", value = "Consequence")
            @QueryParam("filter.variantConsequence") String consequence
    );

    @GET
    @Path("/species/{species}")
    @ApiOperation(value = "Retrieve all alleles of a given species")
    @JsonView(value = {View.GeneAllelesAPI.class})
    JsonResultResponse<Allele> getAllelesPerSpecies(
            @ApiParam(name = "taxonID", value = "Species identifier: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", type = "String")
            @PathParam("species") String species,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(value = "Field name by which to sort", allowableValues = "symbol,name")
            @DefaultValue("symbol") @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc
    );


}
