package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.api.model.AutoCompleteResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/search_autocomplete")
@Api(value = "Search Auto Complete")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AutoCompleteRESTInterface {

    @GET
    @ApiOperation(value = "Searches Autocomplete fields", notes="Search Auto Complete Notes")
    public AutoCompleteResult searchAutoComplete(
            @ApiParam(name = "q", value = "This is what we search for")
            @QueryParam("q") String q,
            @ApiParam(name = "category", value = "This is the category we search in")
            @QueryParam("category") String category
    );
    
}
