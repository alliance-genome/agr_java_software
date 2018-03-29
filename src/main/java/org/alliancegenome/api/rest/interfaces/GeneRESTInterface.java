package org.alliancegenome.api.rest.interfaces;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/gene")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GeneRESTInterface {

    @GET
    @Path("/{id}")
    public Map<String, Object> getGene(
            @PathParam("id") String id
    );
    
}
