package org.alliancegenome.indexer.indexers.curation.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.ws.rs.*;
import org.alliancegenome.curation_api.interfaces.base.BaseIdCrudInterface;
import org.alliancegenome.curation_api.model.entities.GenePhenotypeAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.View;

import java.util.HashMap;

@Path("/gene-phenotype-annotation")
@Produces({"application/json"})
@Consumes({"application/json"})
public interface GenePhenotypeAnnotationInterface extends BaseIdCrudInterface<GenePhenotypeAnnotation> {

	@POST
	@Path("/findForPublic")
	@JsonView({View.PhenotypeAnnotationView.class})
	SearchResponse<GenePhenotypeAnnotation> findForPublic(@DefaultValue("0") @QueryParam("page") Integer page, @DefaultValue("10") @QueryParam("limit") Integer limit, HashMap<String, Object> params);

}
