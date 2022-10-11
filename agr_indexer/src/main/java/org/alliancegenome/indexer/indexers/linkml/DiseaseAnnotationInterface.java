package org.alliancegenome.indexer.indexers.linkml;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.interfaces.base.BaseIdCrudInterface;
import org.alliancegenome.curation_api.interfaces.crud.AlleleDiseaseAnnotationCrudInterface;
import org.alliancegenome.curation_api.interfaces.crud.DiseaseAnnotationCrudInterface;
import org.alliancegenome.curation_api.interfaces.crud.GeneDiseaseAnnotationCrudInterface;
import org.alliancegenome.curation_api.model.entities.DiseaseAnnotation;
import org.alliancegenome.curation_api.response.ObjectResponse;
import org.alliancegenome.curation_api.response.SearchResponse;
import org.alliancegenome.curation_api.view.View;
import org.alliancegenome.indexer.RestConfig;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import si.mazi.rescu.RestProxyFactory;

import javax.ws.rs.*;
import java.util.HashMap;


@Path("/disease-annotation")
@Tag(
	name = "CRUD - Disease Annotations"
)
@Produces({"application/json"})
@Consumes({"application/json"})
public interface DiseaseAnnotationInterface extends BaseIdCrudInterface<DiseaseAnnotation> {

	@POST
	@Path("/find")
	@Tag(
		name = "Database Search Endpoints"
	)
	@JsonView({View.FieldsAndLists.class})
	SearchResponse<DiseaseAnnotation> find(@DefaultValue("0") @QueryParam("page") Integer page, @DefaultValue("10") @QueryParam("limit") Integer limit, @RequestBody HashMap<String, Object> params);

}
