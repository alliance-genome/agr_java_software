package org.alliancegenome.api.controller;

import java.io.InputStream;
import java.util.List;

import org.alliancegenome.api.dto.DownloadFile;
import org.alliancegenome.api.rest.interfaces.DownloadRESTInterface;
import org.alliancegenome.api.service.DownloadService;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;

@RequestScoped
@Slf4j
public class DownloadController implements DownloadRESTInterface {

	@Inject DownloadService downloadService;

	@Context UriInfo uriInfo;
	@Context HttpHeaders headers;

	@Override
	public Response download(String filename) {
		try {
			String release = downloadService.getCurrentRelease();
			InputStream is = downloadService.openDownloadStream(release, filename);
			return Response.ok(is)
					.type(MediaType.APPLICATION_OCTET_STREAM)
					.header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
					.build();
		} catch (Exception e) {
			log.error("Failed to stream download: {}", filename, e);
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@Override
	public List<DownloadFile> listDownloads() {
		try {
			String release = downloadService.getCurrentRelease();
			return downloadService.listDownloads(release, resolvePublicBaseUrl());
		} catch (Exception e) {
			log.error("Failed to list downloads", e);
			throw new RuntimeException(e);
		}
	}

	/** Build the public scheme://host[:port] honouring X-Forwarded-{Proto,Host} so the URL behind a load balancer is correct. */
	private String resolvePublicBaseUrl() {
		String scheme = headers.getHeaderString("X-Forwarded-Proto");
		String host = headers.getHeaderString("X-Forwarded-Host");
		if (scheme == null || scheme.isEmpty()) {
			scheme = uriInfo.getBaseUri().getScheme();
		}
		if (host == null || host.isEmpty()) {
			host = uriInfo.getBaseUri().getAuthority();
		}
		return scheme + "://" + host;
	}

}
