package org.alliancegenome.indexer.indexers.linkml;

import com.fasterxml.jackson.annotation.JsonView;
import org.alliancegenome.curation_api.interfaces.base.BaseIdCrudInterface;
import org.alliancegenome.curation_api.model.entities.AlleleDiseaseAnnotation;
import org.alliancegenome.curation_api.model.entities.GeneDiseaseAnnotation;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.View;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.ws.rs.*;
import java.util.HashMap;


@Path("/allele-disease-annotation")
@Tag(
	name = "CRUD - Disease Annotations"
)
@Produces({"application/json"})
@Consumes({"application/json"})
public interface AlleleDiseaseAnnotationInterface extends BaseIdCrudInterface<AlleleDiseaseAnnotation> {

	@POST
	@Path("/find")
	@Tag(
		name = "Database Search Endpoints"
	)
	@JsonView({View.FieldsAndLists.class})
	SearchResponse<AlleleDiseaseAnnotation> find(@DefaultValue("0") @QueryParam("page") Integer page, @DefaultValue("10") @QueryParam("limit") Integer limit, @RequestBody HashMap<String, Object> params);

}
