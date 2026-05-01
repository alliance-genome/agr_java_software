package org.alliancegenome.api.filters;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;

/**
 * Exposes the public download endpoints at top-level URLs (`/download/{filename}` and `/downloads`) by rerouting them internally to the JAX-RS application path (`/api/...`). The Vert.x `reroute` is an internal forward — the original request path is preserved in access logs while RESTEasy still dispatches via its `@ApplicationPath("/api")`.
 */
@ApplicationScoped
@Slf4j
public class DownloadPathRewriter {

	void init(@Observes Router router) {
		log.info("Registering top-level routes: /downloads, /download/*");
		router.route("/downloads").handler(ctx -> ctx.reroute("/api/downloads"));
		router.route("/download/*").handler(ctx -> ctx.reroute("/api" + ctx.request().path()));
	}

}
