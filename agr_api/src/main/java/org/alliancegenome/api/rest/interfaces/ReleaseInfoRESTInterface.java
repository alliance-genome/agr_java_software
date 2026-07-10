package org.alliancegenome.api.rest.interfaces;

import org.alliancegenome.core.document.ReleaseInfoDocument;
import org.alliancegenome.core.view.PublicView;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/releaseInfo")
@Tag(name = "Release Info")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ReleaseInfoRESTInterface {
	
	@GET
	@Path("/")
	@Operation(summary = "Retrieve release information")
	@JsonView({PublicView.ReleaseInfo.class})
	ReleaseInfoDocument getReleaseInfo();

}





