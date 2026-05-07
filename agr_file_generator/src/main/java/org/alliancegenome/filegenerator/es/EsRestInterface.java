package org.alliancegenome.filegenerator.es;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface EsRestInterface {

	@POST
	@Path("/{index}/_search")
	Map<String, Object> startScroll(
			@PathParam("index") String index,
			@QueryParam("scroll") String scrollDuration,
			Map<String, Object> body);

	@POST
	@Path("/_search/scroll")
	Map<String, Object> continueScroll(Map<String, Object> body);

	@POST
	@Path("/{index}/_count")
	Map<String, Object> count(
			@PathParam("index") String index,
			Map<String, Object> body);

	@POST
	@Path("/{index}/_search")
	Map<String, Object> search(
			@PathParam("index") String index,
			Map<String, Object> body);
}
