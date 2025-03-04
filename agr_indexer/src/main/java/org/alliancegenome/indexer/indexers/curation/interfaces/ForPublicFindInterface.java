package org.alliancegenome.indexer.indexers.curation.interfaces;

import java.util.HashMap;

import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

public interface ForPublicFindInterface<E> {

	@POST
	@Path("/findForPublic")
	SearchResponse<E> findForPublic(
		@DefaultValue("0") @QueryParam("page") Integer page,
		@DefaultValue("10") @QueryParam("limit") Integer limit,
		@DefaultValue("ForPublic") @QueryParam("view") String view,
		HashMap<String, Object> params);
	
	@POST
	@Path("/findForPublic")
	@JsonView({ View.ForPublic.class })
	SearchResponse<E> findForPublic(
		@DefaultValue("0") @QueryParam("page") Integer page,
		@DefaultValue("10") @QueryParam("limit") Integer limit,
		HashMap<String, Object> params);
	
}
