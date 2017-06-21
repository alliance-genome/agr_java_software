package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/gene")
@Api(value = "Search Gene")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GeneRESTInterface {

	@POST
	public void createGene(@HeaderParam("api_access_token") String api_access_token);

	@POST
	@Path("/batch")
	public void createGeneBatch(@HeaderParam("api_access_token") String api_access_token);

	@PUT
	public void updateGene(@HeaderParam("api_access_token") String api_access_token);

	@GET
	@ApiOperation(value = "Gets a Gene by ID", notes="Gene Notes")
	public void getGene(
			@QueryParam("primaryId") String primaryId,
			@QueryParam("symbol") String symbol,
			@QueryParam("soTermId") String soTermId,
			@QueryParam("taxonId") String taxonId,
			@QueryParam("name") String name);

	@DELETE
	@Path("/{id}")
	public void deleteGene(@HeaderParam("api_access_token") String api_access_token, @PathParam("id") Long id);
}
