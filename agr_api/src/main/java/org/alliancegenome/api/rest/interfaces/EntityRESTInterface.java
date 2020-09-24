package org.alliancegenome.api.rest.interfaces;

import java.util.Map;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
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
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Search for an EntityID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id
    );
    
}
