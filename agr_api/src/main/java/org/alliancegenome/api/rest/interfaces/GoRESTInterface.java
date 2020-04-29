package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

@Path("/go")
//@Api(value = "Go Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GoRESTInterface {

    @GET
    @Path("/{id}")
    @Operation(description = "Searches for a Go fields", summary="Go Notes", hidden = true)
    public GOTerm getGo(
            @Parameter(in=ParameterIn.PATH, name = "id", description = "Search for a Go Term by ID", required=true, schema = @Schema(type = SchemaType.STRING))
            @PathParam("id") String id
    );
    
}
