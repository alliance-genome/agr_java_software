package org.alliancegenome.indexer.rest.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
import org.alliancegenome.curation_api.model.entities.Vocabulary;
import org.alliancegenome.curation_api.model.entities.VocabularyTerm;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.View;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.ws.rs.*;
import java.util.HashMap;

@Path("/vocabularyterm")
@Tag(
	name = "CRUD - Vocabulary"
)
@Produces({"application/json"})
@Consumes({"application/json"})
public interface VocabularyRESTInterface {

	@POST
	@Path("/find")
	@Tag(
		name = "Database Search Endpoints"
	)
	@JsonView({View.FieldsAndLists.class})
	SearchResponse<VocabularyTerm> find(@DefaultValue("0") @QueryParam("page") Integer page, @DefaultValue("10") @QueryParam("limit") Integer limit, @RequestBody HashMap<String, Object> params);

}
