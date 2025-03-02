package org.alliancegenome.indexer.indexers.curation.interfaces;

import org.alliancegenome.curation_api.model.entities.GeneToGeneParalogy;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/paralogy")
@Produces({"application/json"})
@Consumes({"application/json"})
public interface GeneToGeneParalogyInterface extends ForPublicFindInterface<GeneToGeneParalogy> {

}
