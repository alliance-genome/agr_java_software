package org.alliancegenome.indexer.indexers.curation.interfaces;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.response.SearchResponse;

import java.util.HashMap;

@Path("/gene")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GeneSummaryInterface {
	@POST
	@Path("/findForPublic")
	SearchResponse<Gene> findForPublic(
		@DefaultValue("0") @QueryParam("page") Integer page,
		@DefaultValue("10") @QueryParam("limit") Integer limit,
		HashMap<String, Object> params);

	@POST
	@Path("/findForPublic")
	SearchResponse<Gene> findForPublic(
		@DefaultValue("0") @QueryParam("page") Integer page,
		@DefaultValue("10") @QueryParam("limit") Integer limit,
		@DefaultValue("ForPublic") @QueryParam("view") String view,
		HashMap<String, Object> params);
}
