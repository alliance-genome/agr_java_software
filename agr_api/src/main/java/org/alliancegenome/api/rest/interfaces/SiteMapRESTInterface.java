package org.alliancegenome.api.rest.interfaces;

import org.alliancegenome.api.model.xml.SiteMapIndex;
import org.alliancegenome.api.model.xml.XMLURLSet;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
@Tag(name = "Site Map")
@Produces(MediaType.APPLICATION_XML)
public interface SiteMapRESTInterface {

	@GET
	@Path("/sitemap.xml")
	SiteMapIndex getSiteMap();

	@GET
	@Path("/sitemap/{siteMapId}.xml")
	XMLURLSet getSiteMap(@PathParam("siteMapId") String siteMapId);

	@GET
	@Path("/accession/{id}")
	Response getAccessionURL(@PathParam("id") String id);

}
