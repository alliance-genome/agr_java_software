package org.alliancegenome.indexer.indexers.curation.interfaces;

import org.alliancegenome.curation_api.interfaces.base.crud.BaseReadCurieControllerInterface;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/ecoterm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface EcoTermRESTInterface extends BaseReadCurieControllerInterface<ECOTerm> {

}
