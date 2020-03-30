package org.alliancegenome.api.rest.interfaces;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("entity")
@Tag(name = "Entity Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface EntityRESTInterface {

    @GET
    @Path("/{id}")
    @Operation(description = "Searches for any Entity", summary = "Entity Notes", hidden = true)
    public Map<String, Object> getEntity(
            //@ApiParam(name = "id", value = "Search for an entity ID")
            @PathParam("id") String id
    );
    
}
