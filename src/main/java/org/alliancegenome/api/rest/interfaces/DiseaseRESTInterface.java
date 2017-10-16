package org.alliancegenome.api.rest.interfaces;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.alliancegenome.api.model.SearchResult;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("/disease")
@Api(value = "Disease Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DiseaseRESTInterface {

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Searches for a Disease fields", notes = "Disease Notes")
    public Map<String, Object> getDisease(
            @ApiParam(name = "id", value = "Search for Disease ID")
            @PathParam("id") String id
    );

    @GET
    @Path("/{id}/associations")
    @ApiOperation(value = "Searches for a Disease fields", notes = "Disease Notes")
    public SearchResult getDiseaseAnnotationsSorted(
            @ApiParam(name = "id", value = "Search for Disease ID")
            @PathParam("id") String id,

            @DefaultValue("20") @QueryParam("limit") int limit,

            @ApiParam(name = "page", value = "This specifies which (page size) to request for")
            @DefaultValue("1") @QueryParam("page") int page,

            @ApiParam(name = "sortBy", value = "This specifies which column to sort by")
            @QueryParam("sortBy") String sortBy,

            @ApiParam(name = "asc", value = "This specifies if the column to be sorted should be ascending or descending")
            @QueryParam("asc") String asc);

    @GET
    @Path("/{id}/associations/download")
    @ApiOperation(value = "Searches for a Disease fields", notes = "Disease Notes")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDiseaseAnnotationsDownloadFile(
            @ApiParam(name = "id", value = "Search for Disease ID")
            @PathParam("id") String id);

    @GET
    @Path("/{id}/associations/downloads")
    @ApiOperation(value = "Searches for a Disease fields", notes = "Disease Notes")
    @Produces(MediaType.TEXT_PLAIN)
    public String getDiseaseAnnotationsDownload(
            @ApiParam(name = "id", value = "Search for Disease ID")
            @PathParam("id") String id);

}
