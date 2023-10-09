package org.alliancegenome.api.application;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api")
@OpenAPIDefinition(
	info = @Info(
		description = "This is the Alliance Genome Java API for access to the Data",
		title = "Alliance of Genome Resources API",
		version = "1.0 Beta"
	)
)
public class RestApplication extends Application {

}