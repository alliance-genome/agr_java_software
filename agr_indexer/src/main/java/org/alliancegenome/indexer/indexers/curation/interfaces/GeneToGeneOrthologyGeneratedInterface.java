package org.alliancegenome.indexer.indexers.curation.interfaces;

import java.util.HashMap;

import org.alliancegenome.curation_api.interfaces.base.BaseIdCrudInterface;
import org.alliancegenome.curation_api.model.entities.orthology.GeneToGeneOrthologyGenerated;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

@Path("/orthologygenerated")
@Produces({"application/json"})
@Consumes({"application/json"})
public interface GeneToGeneOrthologyGeneratedInterface extends BaseIdCrudInterface<GeneToGeneOrthologyGenerated> {

	@POST
	@Path("/findForPublic")
	@JsonView({View.ForPublic.class})
	SearchResponse<GeneToGeneOrthologyGenerated> find(@DefaultValue("0") @QueryParam("page") Integer page, @DefaultValue("10") @QueryParam("limit") Integer limit, HashMap<String, Object> params);

}
