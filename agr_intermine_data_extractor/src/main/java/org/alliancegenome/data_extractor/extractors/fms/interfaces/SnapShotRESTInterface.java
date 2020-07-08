package org.alliancegenome.data_extractor.extractors.fms.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.data_extractor.extractors.fms.SnapShotResponce;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/snapshot")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "SnapShot Endpoints")
public interface SnapShotRESTInterface {

    @GET
    @Path("/release/{releaseVersion}")
    public SnapShotResponce getSnapShot(@PathParam(value = "releaseVersion") String releaseVersion);


}