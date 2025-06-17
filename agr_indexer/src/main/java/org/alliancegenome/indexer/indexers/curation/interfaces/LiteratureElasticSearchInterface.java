package org.alliancegenome.indexer.indexers.curation.interfaces;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;

@ClientHeaderParam(name = "Cache-Control", value = "no-cache")
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public interface LiteratureElasticSearchInterface {
	
	@GET
	@Path("/{index}/_search")
	Map<String, Object> search(
		@PathParam("index") String index,
		@DefaultValue("0") @QueryParam("from") Integer from,
		@DefaultValue("10") @QueryParam("size") Integer size);

	@POST
	@Path("/{index}/_search")
	@Consumes(MediaType.APPLICATION_JSON)
	Map<String, Object> startScroll(
		@PathParam("index") String index,
		@QueryParam("scroll") String scrollTime,
		Map<String, Object> queryBody
	);
	
	@POST
	@Path("/_search/scroll")
	@Consumes(MediaType.APPLICATION_JSON)
	Map<String, Object> continueScroll(Map<String, Object> scrollBody);
	
	@DELETE
	@Path("/_search/scroll")
	@Consumes(MediaType.APPLICATION_JSON)
	void clearScroll(Map<String, Object> clearBody);
	
	@POST
	@Path("/{index}/_search")
	@Consumes(MediaType.APPLICATION_JSON)
	Map<String, Object> searchAfterQuery(
		@PathParam("index") String index,
		Map<String, Object> searchAfterBody
	);
	
	@GET
	@Path("/{index}/_count")
	Map<String, Object> count(@PathParam("index") String index);
}

