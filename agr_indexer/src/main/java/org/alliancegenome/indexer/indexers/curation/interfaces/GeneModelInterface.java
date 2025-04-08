package org.alliancegenome.indexer.indexers.curation.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.alliancegenome.curation_api.model.entities.GenePhenotypeAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.View;

import java.util.HashMap;

@Path("/model")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GeneModelInterface {
	@POST
	@Path("/gene-documents")
	@JsonView({ View.ForPublic.class })
	SearchResponse<ModelDocument> findForPublic(
		@DefaultValue("0") @QueryParam("page") Integer page,
		@DefaultValue("10") @QueryParam("limit") Integer limit,
		HashMap<String, Object> params);
}
