package org.alliancegenome.indexer.indexers.curation.interfaces;

import org.alliancegenome.curation_api.model.entities.VocabularyTerm;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/vocabularyterm")
@Produces({ "application/json" })
@Consumes({ "application/json" })
public interface VocabularyTermRESTInterface extends ForPublicFindInterface<VocabularyTerm> {

}
