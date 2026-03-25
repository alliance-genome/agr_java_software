package org.alliancegenome.indexer.indexers.curation.interfaces;

import java.util.HashMap;

import org.alliancegenome.curation_api.model.entities.Species;
import org.alliancegenome.curation_api.response.SearchResponse;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/species")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SpeciesInterface {
	@POST
	@Path("/findForPublic")
	SearchResponse<Species> findForPublic(
		@DefaultValue("0") @QueryParam("page") Integer page,
		@DefaultValue("100") @QueryParam("limit") Integer limit,
		@DefaultValue("FieldsOnly") @QueryParam("view") String view,
		HashMap<String, Object> params);
}
