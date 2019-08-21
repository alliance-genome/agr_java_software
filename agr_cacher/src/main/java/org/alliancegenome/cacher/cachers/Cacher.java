package org.alliancegenome.cacher.cachers;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.util.ProcessDisplayHelper;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.configuration.cache.CacheMode;

public abstract class Cacher extends Thread {

    protected abstract void cache();
    private ProcessDisplayHelper display = new ProcessDisplayHelper();

    public void runCache() {
        try {
            cache();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public void run() {
        super.run();
        try {
            cache();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public <T> RemoteCache<String, T> setupCache(String cacheName) {
        
        ConfigurationBuilder cb = new ConfigurationBuilder();

        cb.addServer()
        .host(ConfigHelper.getCacheHost())
        .port(ConfigHelper.getCachePort())
        .socketTimeout(500000)
        .connectionTimeout(500000)
        .tcpNoDelay(true);

        RemoteCacheManager rmc = new RemoteCacheManager(cb.build());
        
        //cache = rmc.administration().withFlags(AdminFlag.PERMANENT).getOrCreateCache(cacherConfig.getCacheName(), cacherConfig.getCacheTemplate());

        org.infinispan.configuration.cache.ConfigurationBuilder cb2 = new org.infinispan.configuration.cache.ConfigurationBuilder();
        
//        cb2.persistence()
//        .passivation(false)
//        .addSingleFileStore()
//            .shared(false)
//            .preload(true)
//            .fetchPersistentState(true)
//            .purgeOnStartup(false)
//            .location("/tmp/" + cacheName)
//            .async()
//               .enabled(true)
//               .threadPoolSize(5);
        
        cb2.jmxStatistics();
        cb2.clustering().cacheMode(CacheMode.LOCAL).create();

        return rmc.administration().getOrCreateCache(cacheName, cb2.build());
    }

    protected void startProcess(String message, int totalSize) {
        display.startProcess(message, totalSize);
    }

    protected void progress() {
        display.progress();
    }

    protected void finishProcess() {
        display.finishProcess();
    }

}
