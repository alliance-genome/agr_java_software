package org.alliancegenome.indexer.indexers.curation.interfaces;

import org.alliancegenome.curation_api.interfaces.base.crud.BaseReadCurieControllerInterface;
import org.alliancegenome.curation_api.model.entities.Reference;
import org.alliancegenome.curation_api.response.ObjectResponse;
import org.alliancegenome.curation_api.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("/reference")
@Produces({"application/json"})
@Consumes({"application/json"})
public interface ReferenceInterface extends BaseReadCurieControllerInterface<Reference> {

	@GET
	@Path("/{curie}")
	@JsonView({View.FieldsAndLists.class})
	ObjectResponse<Reference> get(@PathParam("curie") String curie);

}
