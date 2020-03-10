package org.alliancegenome.api.rest.interfaces;

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

import org.alliancegenome.es.model.search.SearchApiResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


@Path("/termName")
//@Api(value = "Phenotype Search" , hidden = true)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PhenotypeRESTInterface {

    @GET
    @Path("/{id}")
    public Map<String, Object> getPhenotype(@PathParam("id") String id);

    @GET
    @Path("/{id}/associations")
    public SearchApiResponse getDiseaseAnnotationsSorted(
            @PathParam("id") String id,
            @DefaultValue("20") @QueryParam("limit") int limit,
            @DefaultValue("1") @QueryParam("page") int page,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("geneticEntity") String geneticEntity,
            @QueryParam("geneticEntityType") String geneticEntityType,
            @QueryParam("termName") String disease,
            @QueryParam("reference") String reference,
            @QueryParam("evidenceCode") String evidenceCode,
            @QueryParam("asc") String asc);

    @GET
    @Path("/{id}/associations/download")
    @ApiOperation(value = "Phenotype search download" , hidden = true)
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDiseaseAnnotationsDownloadFile(@PathParam("id") String id);

    @GET
    @Path("/{id}/associations/downloads")
    @ApiOperation(value = "Phenotype search download" , hidden = true)
    @Produces(MediaType.TEXT_PLAIN)
    public String getDiseaseAnnotationsDownload(@PathParam("id") String id);

}
