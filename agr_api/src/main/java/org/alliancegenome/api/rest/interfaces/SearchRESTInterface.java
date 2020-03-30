package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.es.model.search.SearchApiResponse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/search")
@Tag(name = "Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SearchRESTInterface {

    @GET
    @Operation(description = "Searches Searchable Items for the following", summary = "Search Notes")
    public SearchApiResponse search(
        //@ApiParam(name = "q", value = "This is what we search for")
        @QueryParam("q") String q,
        //@ApiParam(name = "category", value = "This is the category we search in")
        @QueryParam("category") String category,
        //@ApiParam(name = "limit", value = "This limits the returned amount of items (page size)")
        @QueryParam("limit") int limit,
        //@ApiParam(name = "offset", value = "This specifies which (page size) to request for")
        @QueryParam("offset") int offset,
        //@ApiParam(name = "sort_by", value = "We will sort the results by this field")
        @QueryParam("sort_by") String sort_by,
        @Context UriInfo uriInfo
    );
}
