package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.api.model.xml.SiteMapIndex;
import org.alliancegenome.api.model.xml.XMLURLSet;

@Path("/")
@Produces(MediaType.APPLICATION_XML)
public interface SiteMapRESTInterface {

    @GET
    @Path("/sitemap.xml")
    public SiteMapIndex getSiteMap(@Context UriInfo uriInfo);

    @GET
    @Path("/sitemap/{category}-sitemap-{page}.xml")
    public XMLURLSet getCategorySiteMap(
        @PathParam("category") String category,
        @PathParam("page") Integer page,
        @Context UriInfo uriInfo
    );

}
