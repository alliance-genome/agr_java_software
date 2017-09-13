package org.alliancegenome.api.rest.interfaces;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.alliancegenome.api.model.MetaData;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/metadata")
@Api(value = "Meta Data")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface MetaDataRESTInterface {

	@GET
	@ApiOperation(value = "Get MetaData from Build", notes="Meta Data Notes")
	public MetaData getMetaData();
}
