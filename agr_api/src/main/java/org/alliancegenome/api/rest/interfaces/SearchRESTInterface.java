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
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/search")
@Tag(name = "Search")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SearchRESTInterface {

    @GET
    @Operation(summary = "Searches Searchable Items for the following")
    public SearchApiResponse search(
        //@ApiParam(name = "q", value = "This is what we search for")
        @Parameter(in=ParameterIn.QUERY, name = "q", description = "This is what we search for", schema = @Schema(type = SchemaType.STRING))
        @QueryParam("q") String q,
        @Parameter(in=ParameterIn.QUERY, name = "category", description = "This is the category we search in" , schema = @Schema(type = SchemaType.STRING))
        @QueryParam("category") String category,
        @Parameter(in=ParameterIn.QUERY, name = "limit", description = "This limits the returned amount of items (page size)" , schema = @Schema(type = SchemaType.INTEGER))
        @QueryParam("limit") Integer limit,
        @Parameter(in=ParameterIn.QUERY, name = "offset", description = "This specifies which (page size) to request for", schema = @Schema(type = SchemaType.INTEGER))
        @QueryParam("offset") Integer offset,
        @Parameter(in=ParameterIn.QUERY, name = "sort_by", description = "We will sort the results by this field" , schema = @Schema(type = SchemaType.STRING))
        @QueryParam("sort_by") String sort_by,
        @Context UriInfo uriInfo
    );
}
