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

import org.alliancegenome.es.model.search.SearchResponse;

import io.swagger.annotations.Api;

@Path("/disease")
@Api(value = "Disease Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DiseaseRESTInterface {

    @GET
    @Path("/{id}")
    public Map<String, Object> getDisease(@PathParam("id") String id);

    @GET
    @Path("/{id}/associations")
    public SearchResponse getDiseaseAnnotationsSorted(
            @PathParam("id") String id,
            @DefaultValue("20") @QueryParam("limit") int limit,
            @DefaultValue("1") @QueryParam("page") int page,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("geneName") String geneName,
            @QueryParam("species") String species,
            @QueryParam("geneticEntity") String geneticEntity,
            @QueryParam("geneticEntityType") String geneticEntityType,
            @QueryParam("disease") String disease,
            @QueryParam("source") String source,
            @QueryParam("reference") String reference,
            @QueryParam("evidenceCode") String evidenceCode,
            @QueryParam("associationType") String associationType,
            @QueryParam("asc") String asc);

    @GET
    @Path("/{id}/associations/download")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDiseaseAnnotationsDownloadFile(@PathParam("id") String id);

    @GET
    @Path("/{id}/associations/downloads")
    @Produces(MediaType.TEXT_PLAIN)
    public String getDiseaseAnnotationsDownload(@PathParam("id") String id);

}
