package org.alliancegenome.indexer.indexers.curation.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.ws.rs.*;
import org.alliancegenome.curation_api.interfaces.base.BaseIdCrudInterface;
import org.alliancegenome.curation_api.model.entities.GeneExpressionAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.View;

import java.util.HashMap;

@Path("/gene-expression-annotation")
@Produces({"application/json"})
@Consumes({"application/json"})
public interface GeneExpressionAnnotationInterface extends BaseIdCrudInterface<GeneExpressionAnnotation> {
	@POST
	@Path("/findForPublic")
	@JsonView({View.ForPublic.class})
	SearchResponse<GeneExpressionAnnotation> find(
			@DefaultValue("0") @QueryParam("page") Integer page,
			@DefaultValue("10") @QueryParam("limit") Integer limit,
			HashMap<String, Object> params
	);
}
