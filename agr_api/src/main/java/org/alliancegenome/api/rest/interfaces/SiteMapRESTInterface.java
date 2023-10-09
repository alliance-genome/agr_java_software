package org.alliancegenome.api.rest.interfaces;

import org.alliancegenome.api.model.xml.SiteMapIndex;
import org.alliancegenome.api.model.xml.XMLURLSet;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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
