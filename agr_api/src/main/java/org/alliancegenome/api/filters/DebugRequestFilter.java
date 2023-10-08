package org.alliancegenome.api.filters;

import java.io.IOException;

import org.alliancegenome.core.config.ConfigHelper;

import io.quarkus.logging.Log;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DebugRequestFilter implements ContainerRequestFilter {
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		if(ConfigHelper.getDebug()) {
			Log.info("Request: " + requestContext.getUriInfo().getPath());
		}
	}
}