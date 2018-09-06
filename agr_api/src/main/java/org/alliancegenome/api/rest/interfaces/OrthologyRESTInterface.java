package org.alliancegenome.api.rest.interfaces;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/homologs")
@Api(value = "Homology")
@Consumes(MediaType.APPLICATION_JSON)
public interface OrthologyRESTInterface {

    @GET
    @Path("/{taxonIDOne}/{taxonIDTwo}")
    @ApiOperation(value = "Retrieve homologous gene records for given pair of species")
    String getDoubleSpeciesOrthology(
            @ApiParam(name = "taxonIDOne", value = "Taxon ID for the first gene: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", required = true, type = "String")
            @PathParam("taxonIDOne") String speciesOne,
            @ApiParam(name = "taxonIDTwo", value = "Taxon ID for the second gene: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", required = true, type = "String")
            @PathParam("taxonIDTwo") String speciesTwo,
            @ApiParam(value = "Select a stringency filter", allowableValues = "stringent, moderate, all", defaultValue = "stringent")
            @QueryParam("stringencyFilter") String stringencyFilter,
            @ApiParam(value = "Select a calculation method", allowableValues = "Ensembl Compara, HGNC, Hieranoid, InParanoid, OMA, OrthoFinder, OrthoInspector, PANTHER, PhylomeDB, Roundup, TreeFam, ZFIN")
            @QueryParam("methods") String methods,
            @ApiParam(value = "Number of returned rows")
            @DefaultValue("20") @QueryParam("rows") Integer rows,
            @ApiParam(value = "Starting row")
            @DefaultValue("1") @QueryParam("start") Integer start) throws IOException;

    @GET
    @Path("/{taxonID}")
    @ApiOperation(value = "Retrieve homologus gene records for a given species")
    String getSingleSpeciesOrthology(
            @ApiParam(name = "taxonID", value = "Taxon ID for the gene: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", required = true, type = "String")
            @PathParam("taxonID") String species,
            @ApiParam(value = "Select a stringency filter", allowableValues = "stringent, moderate, all", defaultValue = "stringent")
            @QueryParam("stringencyFilter") String stringencyFilter,
            @ApiParam(value = "Select a calculation method", allowableValues = "Ensembl Compara, HGNC, Hieranoid, InParanoid, OMA, OrthoFinder, OrthoInspector, PANTHER, PhylomeDB, Roundup, TreeFam, ZFIN")
            @QueryParam("methods") String methods,
            @ApiParam(value = "Number of returned rows")
            @DefaultValue("20") @QueryParam("rows") Integer rows,
            @ApiParam(value = "Starting row")
            @DefaultValue("1") @QueryParam("start") Integer start) throws IOException;

    @GET
    @Path("/methods")
    @ApiOperation(value = "Retrieve all methods used for calculation of homology")
    String getAllMethodsCalculations() throws JsonProcessingException;
}
