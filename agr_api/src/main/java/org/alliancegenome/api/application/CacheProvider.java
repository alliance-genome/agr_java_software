package org.alliancegenome.api.application;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.alliancegenome.core.config.CacheConfig;
import org.infinispan.client.hotrod.RemoteCacheManager;

@ApplicationScoped
public class CacheProvider {

    @Produces
    @ApplicationScoped
    public RemoteCacheManager getDefaultCacheManager() {
        return CacheConfig.defaultRemoteCacheManager();
    }
    
}
