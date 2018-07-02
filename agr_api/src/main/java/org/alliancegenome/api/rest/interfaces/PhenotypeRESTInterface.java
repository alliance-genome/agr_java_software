package org.alliancegenome.api.rest.interfaces;

import io.swagger.annotations.Api;
import org.alliancegenome.es.model.search.SearchResult;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/phenotype")
@Api(value = "Phenotype Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PhenotypeRESTInterface {

    @GET
    @Path("/{id}")
    public Map<String, Object> getPhenotype(@PathParam("id") String id);

    @GET
    @Path("/{id}/associations")
    public SearchResult getDiseaseAnnotationsSorted(
            @PathParam("id") String id,
            @DefaultValue("20") @QueryParam("limit") int limit,
            @DefaultValue("1") @QueryParam("page") int page,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("geneticEntity") String geneticEntity,
            @QueryParam("geneticEntityType") String geneticEntityType,
            @QueryParam("phenotype") String disease,
            @QueryParam("reference") String reference,
            @QueryParam("evidenceCode") String evidenceCode,
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
