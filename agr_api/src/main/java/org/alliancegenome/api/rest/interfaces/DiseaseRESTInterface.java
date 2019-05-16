package org.alliancegenome.api.rest.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.alliancegenome.neo4j.view.View;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/disease")
@Api(value = "Disease ")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DiseaseRESTInterface {

    @GET
    @Path("/{id}")
    @JsonView(value = {View.DiseaseAPI.class})
    @ApiOperation(value = "Retrieve a Disease object for a given id")
    public DOTerm getDisease(@PathParam("id") String id);

    @GET
    @Path("/{id}/associations")
    @JsonView(value = {View.DiseaseAnnotationSummary.class})
    @ApiOperation(value = "Retrieve all DiseaseAnnotation records for a given disease id")
    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsSorted(
            @ApiParam(name = "id", value = "Disease by DOID: e.g. DOID:9952", required = true, type = "String")
            @PathParam("id") String id,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(value = "Field / column name by which to sort", allowableValues = "geneName,species,geneticEntity,geneticEntityType,disease,source,reference,evidenceCode,associationType", defaultValue = "geneName")
            @QueryParam("sortBy") String sortBy,
            @ApiParam(value = "filter by gene symbol")
            @QueryParam("filter.geneName") String geneName,
            @ApiParam(value = "filter by species")
            @QueryParam("filter.species") String species,
            @ApiParam(value = "filter by gene genetic Entity")
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @ApiParam(value = "filter by genetic Entity type", allowableValues = "gene,allele")
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @ApiParam(value = "filter by disease")
            @QueryParam("filter.disease") String disease,
            @ApiParam(value = "filter by source")
            @QueryParam("filter.source") String source,
            @ApiParam(value = "filter by reference")
            @QueryParam("filter.reference") String reference,
            @ApiParam(value = "filter by evidence code")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @ApiParam(value = "filter by association type")
            @QueryParam("filter.associationType") String associationType,
            @ApiParam(value = "ascending order: true or false", allowableValues = "true,false", defaultValue = "true")
            @QueryParam("asc") String asc);

    @GET
    @Path("/{id}/associations/download")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Download all DiseaseAnnotation records for a given disease id and sorting / filtering parameters")
    public Response getDiseaseAnnotationsDownloadFile(@PathParam("id") String id);

    @GET
    @Path("/{id}/associations/download/all")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Retrieve all DiseaseAnnotation records for a given disease id disregarding sorting / filtering parameters")
    public String getDiseaseAnnotationsDownload(@PathParam("id") String id);

    @GET
    @Path("")
    @JsonView(value = {View.DiseaseAnnotation.class})
    @ApiOperation(value = "Retrieve all expression records of a given set of geneMap")
    JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsRibbonDetails(
            @ApiParam(name = "geneID", value = "Gene by ID", required = true)
            @QueryParam("geneID") List<String> geneIDs,
            @ApiParam(name = "termID", value = "Term ID by which rollup should happen")
            @QueryParam("termID") String termID,
            @ApiParam(name = "filter.species", value = "Species by taxon ID", type = "String")
            @QueryParam("filter.species") String filterSpecies,
            @ApiParam(name = "filter.gene", value = "Gene symbol", type = "String")
            @QueryParam("filter.gene") String filterGene,
            @ApiParam(name = "filter.reference", value = "Reference", type = "String")
            @QueryParam("filter.reference") String filterReference,
            @ApiParam(name = "filter.disease", value = "Ontology term name", type = "String")
            @QueryParam("filter.disease") String diseaseTerm,
            @ApiParam(name = "filter.source", value = "Source", type = "String")
            @QueryParam("filter.source") String filterSource,
            @ApiParam(name = "filter.geneticEntity", value = "geneticEntity", type = "String")
            @QueryParam("filter.geneticEntity") String geneticEntity,
            @ApiParam(name = "filter.geneticEntityType", value = "geneticEntityType", type = "String")
            @QueryParam("filter.geneticEntityType") String geneticEntityType,
            @ApiParam(name = "filter.associationType", value = "associationType", type = "String")
            @QueryParam("filter.associationType") String associationType,
            @ApiParam(name = "filter.evidenceCode", value = "evidenceCode", type = "String")
            @QueryParam("filter.evidenceCode") String evidenceCode,
            @ApiParam(name = "limit", value = "Number of rows returned", defaultValue = "20")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @ApiParam(name = "page", value = "Page number")
            @DefaultValue("1") @QueryParam("page") int page,
            @ApiParam(name = "sortBy", value = "Sort by field name")
            @QueryParam("sortBy") String sortBy,
            @ApiParam(name = "asc", allowableValues = "true,false", value = "ascending or descending")
            @QueryParam("asc") String asc
    ) throws JsonProcessingException;

}
