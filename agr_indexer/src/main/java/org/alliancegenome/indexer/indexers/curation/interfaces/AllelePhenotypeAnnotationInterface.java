package org.alliancegenome.indexer.indexers.curation.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.ws.rs.*;
import org.alliancegenome.curation_api.interfaces.base.BaseIdCrudInterface;
import org.alliancegenome.curation_api.model.entities.AllelePhenotypeAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.View;

import java.util.HashMap;

@Path("/allele-phenotype-annotation")
@Produces({"application/json"})
@Consumes({"application/json"})
public interface AllelePhenotypeAnnotationInterface extends BaseIdCrudInterface<AllelePhenotypeAnnotation> {

	@POST
	@Path("/findForPublic")
	@JsonView({View.PhenotypeAnnotationView.class})
	SearchResponse<AllelePhenotypeAnnotation> findForPublic(@DefaultValue("0") @QueryParam("page") Integer page, @DefaultValue("10") @QueryParam("limit") Integer limit, HashMap<String, Object> params);

}
