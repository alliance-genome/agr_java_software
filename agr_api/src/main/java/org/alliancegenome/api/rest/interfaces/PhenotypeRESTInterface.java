package org.alliancegenome.api.rest.interfaces;

import java.util.Map;

import org.alliancegenome.es.model.search.SearchApiResponse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/termName")
@Tag(name = "Phenotype Search") // hidden = true
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PhenotypeRESTInterface {

	@GET
	@Path("/{id}")
	public Map<String, Object> getPhenotype(@PathParam("id") String id);

	@GET
	@Path("/{id}/associations")
	public SearchApiResponse getDiseaseAnnotationsSorted(
			@PathParam("id") String id,
			@DefaultValue("20") @QueryParam("limit") Integer limit,
			@DefaultValue("1") @QueryParam("page") Integer page,
			@QueryParam("sortBy") String sortBy,
			@QueryParam("geneticEntity") String geneticEntity,
			@QueryParam("geneticEntityType") String geneticEntityType,
			@QueryParam("termName") String disease,
			@QueryParam("reference") String reference,
			@QueryParam("evidenceCode") String evidenceCode,
			@QueryParam("asc") String asc);

	@GET
	@Path("/{id}/associations/download")
	@Operation(summary = "Phenotype search download" , hidden = true)
	@Produces(MediaType.TEXT_PLAIN)
	public Response getDiseaseAnnotationsDownloadFile(@PathParam("id") String id);

	@GET
	@Path("/{id}/associations/downloads")
	@Operation(summary = "Phenotype search download" , hidden = true)
	@Produces(MediaType.TEXT_PLAIN)
	public String getDiseaseAnnotationsDownload(@PathParam("id") String id);

}
