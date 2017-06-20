package org.alliancegenome.api.rest;

import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/search_autocomplete")
@Api(description = "Search Auto Complete", value = "Auto Complete", position = 0)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SearchAutoCompleteRESTInterface {

	@GET
	@ApiOperation(value = "Searches Autocomplete fields", notes="Search Auto Complete Notes")
	public JsonObject searchAutoComplete(
			@ApiParam(name = "q", value = "This is what we search for")
			@QueryParam("q") String q,
			@ApiParam(name = "category", value = "This is the category we search for")
			@QueryParam("category") String category
	);
	
}
