package org.alliancegenome.api.rest.interfaces;

import java.util.List;

import org.alliancegenome.api.dto.DownloadFile;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/")
@Tag(name = "Downloads")
public interface DownloadRESTInterface {

	@GET
	@Path("/download/{filename}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(summary = "Stream a download file for the current Alliance release. Looks up the current release version from Elasticsearch and proxies the file from download.alliancegenome.org/{release}/downloads/{filename}.")
	Response download(@PathParam("filename") String filename);

	@GET
	@Path("/downloads")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(summary = "List the downloadable files for the current Alliance release. Returns a JSON array compatible with what the UI consumed previously from FMS.")
	List<DownloadFile> listDownloads();

}
