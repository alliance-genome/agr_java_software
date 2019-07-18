package org.alliancegenome.cache;

import lombok.extern.log4j.Log4j2;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

import java.io.File;
import java.util.ArrayList;

@Log4j2
public final class AllianceCacheManager {

    private static PersistentCacheManager persistentCacheManager = null;
    private static boolean web;

    private synchronized static void setupCache() {
        if (persistentCacheManager != null)
            return;

        log.info("Setting up persistent cache Manager: ");
        File rootDirectory = new File(".", "ehcache-data.1");
        if (web)
            rootDirectory = new File("../.", "ehcache-data");
        System.out.println("ehcache directory: " + rootDirectory.getAbsolutePath());
        CacheManagerBuilder<PersistentCacheManager> cacheManagerBuilder = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(rootDirectory));
        // create individual cache name spaces
        for (CacheAlliance cache : CacheAlliance.values()) {
            cacheManagerBuilder = cacheManagerBuilder.withCache(cache.getCacheName(), CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, ArrayList.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder().disk(4, MemoryUnit.GB, true))
            );
        }
        persistentCacheManager = cacheManagerBuilder.build(true);
    }

    public static <T> Cache<String, ArrayList> getCacheSpace(CacheAlliance cache) {
        if (persistentCacheManager == null)
            setupCache();
        return persistentCacheManager.getCache(cache.getCacheName(), String.class, ArrayList.class);
    }

    public static Cache<String, ArrayList> getCacheSpaceWeb(CacheAlliance cache) {
        if (persistentCacheManager == null) {
            web = true;
            setupCache();
        }
        return persistentCacheManager.getCache(cache.getCacheName(), String.class, ArrayList.class);
    }

    public static void close() {
        persistentCacheManager.close();
        log.info("closing cache");
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }
}
