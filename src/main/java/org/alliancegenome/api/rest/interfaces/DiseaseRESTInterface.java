package org.alliancegenome.api.rest.interfaces;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/disease")
@Api(value = "Disease Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DiseaseRESTInterface {

	@GET
	@Path("/{id}")
	@ApiOperation(value = "Searches for a Disease fields", notes="Disease Notes")
	public Map<String, Object> getDisease(
			@ApiParam(name = "id", value = "Search for Disease ID")
			@PathParam("id") String id
	);
	
}
