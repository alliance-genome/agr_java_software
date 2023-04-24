package org.alliancegenome.api.filters;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import org.alliancegenome.core.config.ConfigHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class DebugRequestFilter implements ContainerRequestFilter {
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		if(ConfigHelper.getDebug()) {
			log.info("Request: " + requestContext.getUriInfo().getPath());
		}
	}
}