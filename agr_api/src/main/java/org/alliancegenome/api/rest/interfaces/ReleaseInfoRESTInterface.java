package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.neo4j.entity.ReleaseSummary;
import org.alliancegenome.neo4j.entity.node.AllianceReleaseInfo;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.annotation.JsonView;

@Path("/releaseInfo")
@Tag(name = "Release Info")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ReleaseInfoRESTInterface {
    
    @GET
    @Path("/")
    @Operation(summary = "Retrieve release information")
    @JsonView({View.ReleaseInfo.class})
    AllianceReleaseInfo getReleaseInfo();
    
    @GET
    @Path("/summary")
    @Operation(summary = "Retrieve release information summary")
    @JsonView({View.ReleaseInfo.class})
    ReleaseSummary getReleaseInfoSummary();
    
}





    