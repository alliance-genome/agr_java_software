package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.api.model.xml.*;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/")
@Tag(name = "Site Map")
@Produces(MediaType.APPLICATION_XML)
public interface SiteMapRESTInterface {

	@GET
	@Path("/sitemap.xml")
	public SiteMapIndex getSiteMap();

	@GET
	@Path("/sitemap/{category}-sitemap-{page}.xml")
	public XMLURLSet getCategorySiteMap(
		@PathParam("category") String category,
		@PathParam("page") Integer page
	);

}
