package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.es.model.search.SearchResult;

@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SearchRESTInterface {

    @GET

    public SearchResult search(
        @QueryParam("q") String q,
        @QueryParam("category") String category,
        @QueryParam("limit") int limit,
        @QueryParam("offset") int offset,
        @QueryParam("sort_by") String sort_by,
        @Context UriInfo uriInfo
    );
}
