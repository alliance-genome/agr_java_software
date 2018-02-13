package org.alliancegenome.api.rest.interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.shared.es.model.search.AutoCompleteResult;

@Path("/search_autocomplete")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AutoCompleteRESTInterface {

    @GET
    public AutoCompleteResult searchAutoComplete(
            @QueryParam("q") String q,
            @QueryParam("category") String category
    );
    
}
