package org.alliancegenome.indexer.indexers.curation.interfaces;

import org.alliancegenome.curation_api.interfaces.base.crud.BaseReadCurieControllerInterface;
import org.alliancegenome.curation_api.model.entities.ontology.ECOTerm;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/ecoterm")
@Produces({"application/json"})
@Consumes({"application/json"})
public interface EcoTermRESTInterface extends BaseReadCurieControllerInterface<ECOTerm> {

}
