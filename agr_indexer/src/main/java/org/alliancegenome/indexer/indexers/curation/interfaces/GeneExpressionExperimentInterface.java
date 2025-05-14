package org.alliancegenome.indexer.indexers.curation.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.alliancegenome.curation_api.model.entities.GeneExpressionExperiment;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.View;

import java.util.HashMap;



@Path("/gene-expression-experiment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface GeneExpressionExperimentInterface {
	@POST
	@Path("/findForPublic")
	@JsonView({ View.ForPublic.class })
	SearchResponse<GeneExpressionExperiment> findForPublic(
			@DefaultValue("0") @QueryParam("page") Integer page,
			@DefaultValue("10") @QueryParam("limit") Integer limit,
			HashMap<String, Object> params);
}
