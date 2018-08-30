package org.alliancegenome.api.rest.interfaces;


import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.alliancegenome.es.model.search.SearchResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


@Path("/gene")
@Api(value = "Genes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GeneRESTInterface {

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Retrieve a Gene for given ID")
    Map<String, Object> getGene(
            @ApiParam(name = "id", value = "Gene by ID")
            @PathParam("id") String id
    );

    @GET
    @Path("/{id}/alleles")
    @ApiOperation(value = "Retrieve all alleles of a given gene")
    SearchResult getAllelesPerGene(
            @ApiParam(name = "id", value = "Search for Alleles for a given Gene by ID")
            @PathParam("id") String id
    );

    @GET
    @Path("/{id}/phenotypes")
    @ApiOperation(value = "Retrieve termName annotations for given gene")
    SearchResult getPhenotypeAnnotations(
            @ApiParam(name = "id", value = "Gene by ID", required = true, type = "String")
            @PathParam("id") String id,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(value = "Field name by which to sort", allowableValues = "termName,geneticEntity")
            @DefaultValue("termName") @QueryParam("sortBy") String sortBy,
            @ApiParam(name = "geneticEntity", value = "genetic entity symbol")
            @QueryParam("geneticEntity") String geneticEntity,
            @ApiParam(name = "geneticEntityType", value = "genetic entity type", allowableValues = "allele")
            @QueryParam("geneticEntityType") String geneticEntityType,
            @ApiParam(value = "termName annotation")
            @QueryParam("termName") String phenotype,
            @ApiParam(value = "Reference number: PUBMED or a Pub ID from the MOD")
            @QueryParam("reference") String reference,
            @ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc);

    @GET
    @Path("/{id}/phenotypes/download")
    @ApiOperation(value = "Retrieve all termName annotations for a given gene", notes = "Download all termName annotations for a given gene")
    @Produces(MediaType.TEXT_PLAIN)
    Response getPhenotypeAnnotationsDownloadFile(@ApiParam(name = "id", value = "Gene by ID", required = true, type = "String")
                                                 @PathParam("id") String id);

    @GET
    @Path("/{geneID}/homologs")
    @ApiOperation(value = "Retrieve homologous gene records", notes = "Download homology records.")
    String getGeneOrthology(@ApiParam(name = "geneID", value = "Source Gene ID: the gene for which you are searching homologous gene", required = true, type = "String")
                            @PathParam("geneID") String id,
                            @ApiParam(value = "apply stringency filter", allowableValues = "stringent, moderate, all", defaultValue = "stringent")
                            @DefaultValue("stringent") @QueryParam("stringencyFilter") String stringencyFilter,
                            @ApiParam(value = "taxonID: List of taxon IDs for the genes of the target species")
                            @QueryParam("taxonID") List<String> taxonID,
                            @ApiParam(value = "calculation methods", allowableValues = "Ensembl Compara, HGNC, Hieranoid, InParanoid, OMA, OrthoFinder, OrthoInspector, PANTHER, PhylomeDB, Roundup, TreeFam, ZFIN")
                            @QueryParam("methods") List<String> methods,
                            @ApiParam(value = "maximum number of rows returned")
                            @DefaultValue("20") @QueryParam("rows") Integer rows,
                            @ApiParam(value = "starting row number (for pagination)")
                            @DefaultValue("1") @QueryParam("start") Integer start) throws IOException;


    @GET
    @Path("/{id}/interactions")
    @ApiOperation(value = "Retrieve interations for a given gene")
    String getInteractions(
            @ApiParam(name = "id", value = "Gene ID", required = true)
            @PathParam("id") String id);

    @GET
    @Path("/{id}/expression-summary")
    @ApiOperation(value = "Retrieve all expression records of a given gene")
    String getExpressionSummary(
            @ApiParam(name = "id", value = "Gene by ID", required = true, type = "String")
            @PathParam("id") String id
    ) throws JsonProcessingException;


}
