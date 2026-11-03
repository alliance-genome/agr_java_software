package org.alliancegenome.api.rest.interfaces;

import java.util.List;

import org.alliancegenome.core.document.ResourceDescriptorDocument;
import org.alliancegenome.core.view.PublicView;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/resourceDescriptors")
@Tag(name = "Resource Descriptors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ResourceDescriptorRESTInterface {

	@GET
	@Path("/")
	@Operation(summary = "Retrieve all Resource Descriptors")
	@JsonView({PublicView.ResourceDescriptor.class})
	List<ResourceDescriptorDocument> getResourceDescriptors();

}
