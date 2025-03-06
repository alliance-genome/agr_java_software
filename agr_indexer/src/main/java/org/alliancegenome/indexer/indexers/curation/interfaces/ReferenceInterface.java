package org.alliancegenome.indexer.indexers.curation.interfaces;

import org.alliancegenome.curation_api.interfaces.base.crud.BaseReadCurieControllerInterface;
import org.alliancegenome.curation_api.model.entities.Reference;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/reference")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ReferenceInterface extends BaseReadCurieControllerInterface<Reference> {

}
