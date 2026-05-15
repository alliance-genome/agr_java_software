package org.alliancegenome.data_extractor.extractors.fms.interfaces;

import java.util.List;

import org.alliancegenome.data_extractor.extractors.fms.DataFile;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/datafile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DataFileRESTInterface {

	@GET
	@Path("/{id}")
	DataFile get(@PathParam("id") String id);

	@GET
	@Path("/all")
	List<DataFile> getDataFiles();

	@GET
	@Path("/by/{dataType}")
	List<DataFile> getDataTypeFiles(
		@PathParam("dataType") String dataType,
		@DefaultValue("false") @QueryParam("latest") Boolean latest);

	@GET
	@Path("/by/release/{releaseVersion}")
	List<DataFile> getDataFilesByRelease(
		@PathParam("releaseVersion") String releaseVersion,
		@DefaultValue("false") @QueryParam("latest") Boolean latest);

	@GET
	@Path("/by/{dataType}/{dataSubtype}")
	List<DataFile> getDataTypeSubTypeFiles(
		@PathParam("dataType") String dataType,
		@PathParam("dataSubtype") String dataSubType,
		@DefaultValue("false") @QueryParam("latest") Boolean latest
	);

	@GET
	@Path("/by/{releaseVersion}/{dataType}/{dataSubtype}")
	List<DataFile> getReleaseDataTypeSubTypeFiles(
		@PathParam("releaseVersion") String releaseVersion,
		@PathParam("dataType") String dataType,
		@PathParam("dataSubtype") String dataSubType,
		@DefaultValue("false") @QueryParam("latest") Boolean latest
	);
}
