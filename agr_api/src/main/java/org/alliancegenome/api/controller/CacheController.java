package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.neo4j.view.View;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionType;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.log4j.Log4j2;


@Log4j2
@RequestScoped
@Path("/cache")
@Api(value = "Cache")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CacheController {

    @Inject
    private RemoteCacheManager manager;
    
    @GET
    @Path("/{cache_name}/{id}")
    @ApiOperation(value = "Get Cache Object")
    @JsonView(value = {View.Default.class})
    public String getCacheObject(
            @ApiParam(name = "id", value = "Search for an object by ID")
            @PathParam("id") String id,
            @ApiParam(name = "cache_name", value = "Cache Type to Search by")
            @PathParam("cache_name") String cache_name
    ) {
        
//      org.infinispan.configuration.cache.ConfigurationBuilder cb2 = new org.infinispan.configuration.cache.ConfigurationBuilder();
//
//      cb2
//              .memory()
//              .storageType(StorageType.BINARY)
//              .evictionType(EvictionType.MEMORY)
//              .size(100_000)
//              .persistence()
//              .passivation(false)
//              .addSingleFileStore()
//              .preload(false)
//              .shared(false)
//              .fetchPersistentState(true)
//              .location("/data/" + cache_name).async().enable().threadPoolSize(5);
//      
//      log.debug("Creating Cache: " + cache_name);
//
//      RemoteCache<String, String> cache = manager.administration().getOrCreateCache(cache_name, cb2.build());
        
        RemoteCache<String, String> cache = manager.getCache(cache_name);
        
        return (String)cache.get(id);
    }
}
