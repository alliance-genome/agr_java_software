package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.api.model.SiteMapIndex;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/sitemap")
@Api(value = "Site Map")
@Produces(MediaType.APPLICATION_ATOM_XML)
@Consumes(MediaType.APPLICATION_JSON)
public interface SiteMapRESTInterface {

	@GET
	@Path("/gene")
	@ApiOperation(value = "Searches Searchable Items for the following", notes="Search Notes")
	public SiteMapIndex getGeneSiteMap(@Context UriInfo uriInfo);
}
