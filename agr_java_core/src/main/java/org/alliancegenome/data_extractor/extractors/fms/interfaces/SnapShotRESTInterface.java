package org.alliancegenome.data_extractor.extractors.fms.interfaces;

import org.alliancegenome.data_extractor.extractors.fms.SnapShotResponce;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/snapshot")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "SnapShot Endpoints")
public interface SnapShotRESTInterface {

	@GET
	@Path("/release/{releaseVersion}")
	public SnapShotResponce getSnapShot(@PathParam(value = "releaseVersion") String releaseVersion);


}