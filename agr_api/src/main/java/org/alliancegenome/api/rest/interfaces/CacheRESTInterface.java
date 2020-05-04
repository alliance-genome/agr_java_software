package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/cache")
@Api(value = "Cache")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CacheRESTInterface {

    @GET
    @JsonView(View.Cacher.class)
    @Path("/status")
    JsonResultResponse<CacheStatus> getCacheStatus(
            @DefaultValue("20") @QueryParam("limit") int limit,
            @DefaultValue("1") @QueryParam("page") int page,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("asc") String asc,
            @QueryParam("filter.indexName") String moleculeType
    );

    @GET
    @JsonView(View.CacherDetail.class)
    @Path("/{cacheName}")
    CacheStatus getCacheStatusPerSpace(@PathParam("cacheName") String cacheName);


    @GET
    @Path("/{cacheName}/{id}")
    @ApiOperation(value = "Get Cache Object")
    @JsonView(value = {View.Default.class})
    String getCacheObject(
            @ApiParam(name = "id", value = "Search for an object by ID")
            @PathParam("id") String id,
            @ApiParam(name = "cacheName", value = "Named Cache to Search by")
            @PathParam("cacheName") String cacheName
    );

}
