package org.alliancegenome.api.application;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/api")
@OpenAPIDefinition(
	info = @Info(
		description = """
			This is the Alliance Genome Java API for access to the Data

			**Please note:** A mailing list has been established to alert developers that make use of \
			Alliance of Genome Resources public APIs to any potentially breaking changes to, or new \
			data added to, our APIs. Alerts will be announced by email, but can also be seen on the \
			Alliance Release Notes page:

			[https://www.alliancegenome.org/release-notes](https://www.alliancegenome.org/release-notes)

			Archives of all prior messages to this list will be available at the Archive available at \
			the URL below. Please register/subscribe for this mailing list at the following URL:

			[https://mailman.stanford.edu/mailman/listinfo/alliance-api-changes](https://mailman.stanford.edu/mailman/listinfo/alliance-api-changes)
			""",
		title = "Alliance of Genome Resources API",
		version = "1.0 Beta"
	)
)
public class RestApplication extends Application {

}