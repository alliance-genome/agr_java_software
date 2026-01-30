package org.alliancegenome.indexer.indexers.curation.interfaces;

import org.alliancegenome.curation_api.model.entities.Reference;
import org.alliancegenome.curation_api.response.ObjectResponse;
import org.alliancegenome.curation_api.view.CurationView;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/reference")
@Produces(MediaType.APPLICATION_JSON)
public interface ReferenceInterface {
	@GET
	@Path("/{curie}")
	@JsonView(CurationView.FieldsOnly.class)
	ObjectResponse<Reference> getByCurie(@PathParam("curie") String curie);
}
