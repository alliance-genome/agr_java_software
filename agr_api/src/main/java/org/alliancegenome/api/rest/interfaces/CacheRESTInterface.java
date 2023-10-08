package org.alliancegenome.api.rest.interfaces;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.annotation.JsonView;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/cache")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Cache Search")
public interface CacheRESTInterface {

	@GET
	@JsonView(View.Cacher.class)
	@Path("/status")
	public JsonResultResponse<CacheStatus> getCacheStatus(
			@DefaultValue("20") @QueryParam("limit") int limit,
			@DefaultValue("1") @QueryParam("page") int page,
			@QueryParam("sortBy") String sortBy,
			@QueryParam("asc") String asc,
			@QueryParam("filter.indexName") String moleculeType
	);

	@GET
	@JsonView(View.CacherDetail.class)
	@Path("/{cacheName}")
	public CacheStatus getCacheStatusPerSpace(@PathParam("cacheName") String cacheName);


	@GET
	@Path("/{cacheName}/{id}")
	@Operation(summary = "Get Cache Object")
	@JsonView(value = {View.Default.class})
	public String getCacheEntryString(
			@Parameter(in=ParameterIn.PATH, name = "id", description = "Search for an object by ID", required=true, schema = @Schema(type = SchemaType.STRING))
			@PathParam("id") String id,
			@Parameter(in=ParameterIn.PATH, name = "cacheName", description = "Named Cache to Search by", required=true, schema = @Schema(type = SchemaType.STRING))
			@PathParam("cacheName") String cacheName
	);

}
