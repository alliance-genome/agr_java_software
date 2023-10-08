package org.alliancegenome.api.application;

import org.alliancegenome.core.config.CacheConfig;
import org.infinispan.client.hotrod.RemoteCacheManager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Produces;

@ApplicationScoped
public class CacheProvider {

	@Produces
	@ApplicationScoped
	public RemoteCacheManager getDefaultCacheManager() {
		return CacheConfig.defaultRemoteCacheManager();
	}
	
}
