package org.alliancegenome.indexer.indexers.curation.interfaces;

import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;
import org.alliancegenome.curation_api.response.ObjectResponse;
import org.alliancegenome.curation_api.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/ecoterm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface EcoTermRESTInterface {
	@GET
	@Path("/{curie}")
	@JsonView(View.FieldsOnly.class)
	ObjectResponse<ECOTerm> getByCurie(@PathParam("curie") String curie);
}
