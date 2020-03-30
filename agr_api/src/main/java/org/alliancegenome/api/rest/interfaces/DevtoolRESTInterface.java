package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.api.entity.CacheSummary;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

@Path("/devtool")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DevtoolRESTInterface {

    @GET
    @JsonView(View.Cacher.class)
    @Path("/cache-status")
    CacheSummary getCacheStatus();

    @GET
    @JsonView(View.CacherDetail.class)
    @Path("/cache-status/{cacheSpace}")
    CacheStatus getCacheStatusPerSpace(@PathParam("cacheSpace") String cacheSpace, @QueryParam("entityID") String entityID);

}
