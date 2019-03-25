package org.alliancegenome.api.rest.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
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
    @JsonView(value = {View.DiseaseAnnotation.class})
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

}
