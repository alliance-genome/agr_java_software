package org.alliancegenome.api.rest.interfaces;

import org.alliancegenome.api.entity.CacheSummary;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/devtool")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DevtoolRESTInterface {

    @GET
    @Path("/cache-status")
    public CacheSummary getCacheStatus();

}
