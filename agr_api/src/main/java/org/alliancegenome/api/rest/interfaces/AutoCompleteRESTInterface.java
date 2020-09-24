package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.es.model.search.AutoCompleteResult;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

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
