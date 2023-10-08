package org.alliancegenome.api.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.CacheService;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class CacheStatusService {

	@Inject CacheService cacheService;

	public CacheStatus getCacheStatus(CacheAlliance type) {
		return getCacheStatus(type, null);
	}

	public CacheStatus getCacheStatus(CacheAlliance type, String entityID) {

		final CacheStatus entityCache = cacheService.getCacheEntry(type.getCacheName(), type, CacheStatus.class);
		if (entityID != null)
			entityCache.getEntityStats().keySet().removeIf(id -> !id.contains(entityID));
		return entityCache;
	}

	public Map<CacheAlliance, CacheStatus> getAllCachStatusRecords() {
		Map<CacheAlliance, CacheStatus> map = new HashMap<>();
		Arrays.stream(CacheAlliance.values()).forEach(cacheAlliance -> {
			CacheStatus status;
			try {
				status = getCacheStatus(cacheAlliance, null);
				if (status != null)
					map.put(cacheAlliance, status);
			} catch (Exception e) {
				Log.info("No suitable cache status found for " + cacheAlliance.getCacheName());
			}
		});
		return map;
	}

	public Object getCacheObject(String id, String cacheName) {
		CacheAlliance cache = CacheAlliance.getTypeByName(cacheName);
		if (cache == null)
			return "No Cache with name " + cacheName + " found";
		return cacheService.getCacheEntry(id, cache, cache.getClazz());
	}
	
	public String getCacheEntryString(String id, String cacheName) {
		CacheAlliance cache = CacheAlliance.getTypeByName(cacheName);
		if (cache == null)
			return "No Cache with name " + cacheName + " found";
		return cacheService.getCacheEntryString(id, cache, cache.getClazz());
	}
}
