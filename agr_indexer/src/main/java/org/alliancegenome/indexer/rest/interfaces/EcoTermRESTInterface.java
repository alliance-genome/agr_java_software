package org.alliancegenome.indexer.rest.interfaces;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.ws.rs.*;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;
import org.alliancegenome.curation_api.response.ObjectResponse;
import org.alliancegenome.curation_api.view.View;

@Path("/ecoterm")
@Produces({"application/json"})
@Consumes({"application/json"})
public interface EcoTermRESTInterface {

	@GET
	@Path("/{curie}")
	@JsonView({View.FieldsAndLists.class})
	ObjectResponse<ECOTerm> find(@PathParam("curie") String curie);

}
