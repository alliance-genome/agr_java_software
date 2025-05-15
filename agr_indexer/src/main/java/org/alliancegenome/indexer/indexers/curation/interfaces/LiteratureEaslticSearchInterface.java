package org.alliancegenome.indexer.indexers.curation.interfaces;

import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/references_index")
@Produces(MediaType.APPLICATION_JSON)
//@Consumes(MediaType.APPLICATION_JSON)
public interface LiteratureEaslticSearchInterface {
	
	@GET
	@Path("/_search")
	Map<String, Object> search(
		@QueryParam("from") Integer from,
		@QueryParam("size") Integer size);
	
	@GET
	@Path("/_count")
	Map<String, Object> count();
}
