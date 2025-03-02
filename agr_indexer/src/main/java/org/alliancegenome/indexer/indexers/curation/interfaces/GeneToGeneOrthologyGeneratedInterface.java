package org.alliancegenome.indexer.indexers.curation.interfaces;

import org.alliancegenome.curation_api.model.entities.orthology.GeneToGeneOrthologyGenerated;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/orthologygenerated")
@Produces({"application/json"})
@Consumes({"application/json"})
public interface GeneToGeneOrthologyGeneratedInterface extends ForPublicFindInterface<GeneToGeneOrthologyGenerated> {

}
