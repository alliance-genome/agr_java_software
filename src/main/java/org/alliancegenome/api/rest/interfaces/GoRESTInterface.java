package org.alliancegenome.api.rest.interfaces;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/go")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GoRESTInterface {

    @GET
    @Path("/{id}")
    public Map<String, Object> getGo(@PathParam("id") String id);
    
}
