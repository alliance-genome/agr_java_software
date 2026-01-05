package org.alliancegenome.indexer.indexers.curation.interfaces;

import java.util.HashMap;

import org.alliancegenome.curation_api.model.entities.associations.AlleleConstructAssociation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/alleleconstructassociation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AlleleConstructAssociationInterface {
	@POST
	@Path("/find")
	@JsonView({ View.ForPublic.class })
	SearchResponse<AlleleConstructAssociation> findForPublic(
		@DefaultValue("0") @QueryParam("page") Integer page,
		@DefaultValue("10") @QueryParam("limit") Integer limit,
		HashMap<String, Object> params);
}
