package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.api.entity.CacheSummary;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/cache")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CacheRESTInterface {

    @GET
    @JsonView(View.Cacher.class)
    @Path("/status")
    CacheSummary getCacheStatus();

    @GET
    @JsonView(View.CacherDetail.class)
    @Path("/{cacheSpace}")
    CacheStatus getCacheStatusPerSpace(@PathParam("cacheSpace") String cacheSpace);


    @GET
    @Path("/{cache_name}/{id}")
    @ApiOperation(value = "Get Cache Object")
    @JsonView(value = {View.Default.class})
    String getCacheObject(
            @ApiParam(name = "id", value = "Search for an object by ID")
            @PathParam("id") String id,
            @ApiParam(name = "cache_name", value = "Cache Type to Search by")
            @PathParam("cache_name") String cache_name
    );

}
