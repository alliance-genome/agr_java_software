package org.alliancegenome.indexer.indexers.curation.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.ws.rs.*;
import org.alliancegenome.curation_api.interfaces.base.BaseIdCrudInterface;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.alliancegenome.curation_api.response.ObjectResponse;
import org.alliancegenome.curation_api.view.View;

@Path("/reference")
@Produces({"application/json"})
@Consumes({"application/json"})
public interface ReferenceInterface extends BaseIdCrudInterface<Reference> {

	@GET
	@Path("/{curie}")
	@JsonView({View.FieldsAndLists.class})
	ObjectResponse<Reference> get(@PathParam("curie") String curie);

}
