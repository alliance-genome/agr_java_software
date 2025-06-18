package org.alliancegenome.indexer.indexers.curation.interfaces;

import java.util.Map;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public interface ElasticSearchInterface {

	@GET
	@Path("/{index}/_search")
	Map<String, Object> search(@PathParam("index") String index, @DefaultValue("0") @QueryParam("from") Integer from, @DefaultValue("10") @QueryParam("size") Integer size);

	@GET
	@Path("/{index}/_count")
	Map<String, Object> count(@PathParam("index") String index);
}
