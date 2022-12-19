package org.alliancegenome.indexer.rest.interfaces;

import java.util.HashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.View;

import com.fasterxml.jackson.annotation.JsonView;

@Path("/vocabularyterm")
@Produces({"application/json"})
@Consumes({"application/json"})
public interface VocabularyRESTInterface {

	@POST
	@Path("/find")
	@JsonView({View.FieldsAndLists.class})
	SearchResponse<VocabularyTerm> find(@DefaultValue("0") @QueryParam("page") Integer page, @DefaultValue("10") @QueryParam("limit") Integer limit, HashMap<String, Object> params);

}
