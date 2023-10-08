package org.alliancegenome.api.rest.interfaces;

import org.alliancegenome.es.model.search.AutoCompleteResult;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/search_autocomplete")
@Tag(name = "Search Auto Complete")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AutoCompleteRESTInterface {

	@GET
	public AutoCompleteResult searchAutoComplete(
			@QueryParam("q") String q,
			@QueryParam("category") String category
	);
	
}
