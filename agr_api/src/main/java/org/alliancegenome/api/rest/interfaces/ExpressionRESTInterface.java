package org.alliancegenome.api.rest.interfaces;


import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.entity.RibbonSummary;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.view.View;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("/expression")
@Api(value = "Expression")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ExpressionRESTInterface {

    @GET
    @Path("")
    @JsonView(value = {View.Expression.class})
    @ApiOperation(value = "Retrieve all expression records of a given set of geneMap")
    JsonResultResponse<ExpressionDetail> getExpressionAnnotations(
            @ApiParam(name = "geneID", value = "Gene by ID", required = true)
            @QueryParam("geneID") List<String> geneIDs,
            @ApiParam(name = "termID", value = "Term ID by which rollup should happen")
            @QueryParam("termID") String termID,
            @ApiParam(name = "filter.species", value = "Species by taxon ID", type = "String")
            @QueryParam("filter.species") String filterSpecies,
            @ApiParam(name = "filter.gene", value = "Gene symbol", type = "String")
            @QueryParam("filter.gene") String filterGene,
            @ApiParam(name = "filter.stage", value = "Stage name", type = "String")
            @QueryParam("filter.stage") String filterStage,
            @ApiParam(name = "filter.assay", value = "Assay name", type = "String")
            @QueryParam("filter.assay") String filterAssay,
            @ApiParam(name = "filter.reference", value = "Reference", type = "String")
            @QueryParam("filter.reference") String filterReference,
            @ApiParam(name = "filter.term", value = "Ontology term name", type = "String")
            @QueryParam("filter.term") String filterTerm,
            @ApiParam(name = "filter.source", value = "Source", type = "String")
            @QueryParam("filter.source") String filterSource,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(name = "sortBy", value = "Sort by field name", allowableValues = "Default,Species,Location,Assay,Stage")
            @QueryParam("sortBy") String sortBy,
            @ApiParam(name = "asc", allowableValues = "true,false", value = "ascending or descending")
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
    @ApiOperation(value = "Retrieve all expression records of a given set of geneMap")
    String getExpressionAnnotationsByTaxon(
            @ApiParam(name = "taxonID", value = "Taxon ID for the first gene: Could be the full ID, e.g. 'NCBITaxon:10090', or just the ID, i.e. '10090'. Alternatively, part of a species name uniquely identifying a single species, e.g. 'danio' or 'mus'.", required = true, type = "String")
            @PathParam("taxonID") String speciesOne,
            @ApiParam(name = "termID", value = "Term ID by which rollup should happen")
            @QueryParam("termID") String termID,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page
    ) throws JsonProcessingException;

    @GET
    @Path("/ribbon-summary")
    @JsonView(value = {View.Expression.class})
    @ApiOperation(value = "Retrieve summary of expression for given list of genes")
    RibbonSummary getExpressionSummary(
            @ApiParam(name = "geneID", value = "list of genes for which expression data is requested", required = true)
            @QueryParam("geneID") List<String> geneIDs
    ) ;

}
