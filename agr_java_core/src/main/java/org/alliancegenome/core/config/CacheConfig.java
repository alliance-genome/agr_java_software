package org.alliancegenome.core.config;

import org.alliancegenome.cache.CacheAlliance;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CacheConfig {

	private static RemoteCacheManager manager;
	
	public static RemoteCacheManager defaultRemoteCacheManager() {
		if(manager != null) return manager;
		
		log.info("Setting up cache manager");
		ConfigurationBuilder builder = new ConfigurationBuilder()
				.addServer()
				.host(ConfigHelper.getCacheHost())
				.port(ConfigHelper.getCachePort())
				.security().authentication().saslMechanism("DIGEST-MD5").username("admin").password("admin")
				.socketTimeout(500000)
				.connectionTimeout(500000)
				.statistics()
				.tcpNoDelay(true);
		
		log.info("Creating RemoteCacheManager with Configuration Builder: " + builder);
		manager = new RemoteCacheManager(builder.build());
		log.info("Finished Creating RemoteCacheManager");
		return manager;
	}
	
	public static RemoteCache<String, String> createCache(CacheAlliance cache) {

		if(manager == null) defaultRemoteCacheManager();
		
		log.debug("Creating Cache: " + cache.getCacheName());

		org.infinispan.configuration.cache.ConfigurationBuilder cb2 = new org.infinispan.configuration.cache.ConfigurationBuilder();

		cb2
				.memory()
				.storage(StorageType.OFF_HEAP)
				.maxSize(cache.getCacheSize() + "").expiration().lifespan(-1)
				.persistence()
				.passivation(false)
				.addSingleFileStore()
					.purgeOnStartup(false)
					.preload(false)
					.shared(false)
					.fetchPersistentState(true)
					.location("/opt/infinispan/server/data/" + cache.getCacheName())
				.async()
				.enable();

		RemoteCache<String, String> remoteCache = manager.administration().getOrCreateCache(cache.getCacheName(), cb2.build());

		// log.info("Clearing cache if exists"); // This might need to run if the cacher is running
		// rmc.getCache(cache.getCacheName()).clear(); // But should not be run via the API

		log.info("Cache: " + cache.getCacheName() + " finished creating");
		return remoteCache;
		
	}

	//	public CacheProvider() {
	//		log.info("Setting up cache manager");
	//		ServerConfigurationBuilder builder = new ConfigurationBuilder().addServer().host("localhost").port(11222);
	//		log.info("ServerConfigurationBuilder: " + builder);
	//		manager = new RemoteCacheManager(builder.build());
	//	}
	//
	//	@Override
	//	public RemoteCacheManager getContext(Class<?> type) {
	//		return manager;
	//	}


}
