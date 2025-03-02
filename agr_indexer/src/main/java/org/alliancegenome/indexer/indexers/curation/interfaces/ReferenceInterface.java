package org.alliancegenome.indexer.indexers.curation.interfaces;

import org.alliancegenome.curation_api.interfaces.base.crud.BaseReadCurieControllerInterface;
import org.alliancegenome.curation_api.model.entities.Reference;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/reference")
@Produces({"application/json"})
@Consumes({"application/json"})
public interface ReferenceInterface extends BaseReadCurieControllerInterface<Reference> {

}
