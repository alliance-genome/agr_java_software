package org.alliancegenome.indexer.indexers.curation.interfaces;

import java.util.HashMap;

import org.alliancegenome.curation_api.model.entities.AGMPhenotypeAnnotation;
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

@Path("/agm-phenotype-annotation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AGMPhenotypeAnnotationInterface extends ForPublicFindInterface<AGMPhenotypeAnnotation> {
	@Override
	@POST
	@Path("/findForPublic")
	@JsonView({View.PhenotypeAnnotationView.class})
	SearchResponse<AGMPhenotypeAnnotation> findForPublic(@DefaultValue("0") @QueryParam("page") Integer page, @DefaultValue("10") @QueryParam("limit") Integer limit, HashMap<String, Object> params);
}
