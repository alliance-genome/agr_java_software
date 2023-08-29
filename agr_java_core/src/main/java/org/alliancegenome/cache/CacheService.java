package org.alliancegenome.cache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.core.config.CacheConfig;
import org.alliancegenome.neo4j.view.View;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class CacheService {

	@Inject
	RemoteCacheManager manager;

	public static ObjectMapper mapper = new ObjectMapper();

	// Only the cacher should call this constructor
	// In the context of an application server the manager will be injected
	public CacheService() {
		this.manager = CacheConfig.defaultRemoteCacheManager();
	}

	static {
		mapper = JsonMapper.builder().configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false).build();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public List<String> getAllKeys(CacheAlliance cacheSpace) {
		RemoteCache<String, String> cache = getCacheSpace(cacheSpace);
		return new ArrayList<>(cache.keySet());
	}

	private RemoteCache<String, String> getCacheSpace(CacheAlliance cache) {
		// log.info("Getting Cache Space: " + cache.getCacheName());
		RemoteCache<String, String> remoteCache = manager.getCache(cache.getCacheName());

		if (remoteCache == null) {
			return CacheConfig.createCache(cache);
		}

		return remoteCache;
	}

	public <O> List<O> getCacheEntries(String entityID, CacheAlliance cacheSpace) {
		CollectionType javaType = mapper.getTypeFactory().constructCollectionType(List.class, cacheSpace.getClazz());

		String json = getCacheSpace(cacheSpace).get(entityID);
		if (json == null)
			return null;

		List<O> list;

		try {
			list = new ArrayList<>(mapper.readValue(json, javaType));
		} catch (IOException e) {
			log.error("Error during deserialization ", e);
			throw new RuntimeException(e);
		}

		return list;
	}

	public CacheStatus getCacheStatus(String entityName, CacheAlliance cacheSpace) {
		String json = getCacheSpace(cacheSpace).get(entityName);
		if (json == null)
			return null;

		try {
			return mapper.readerWithView(View.Cacher.class).forType(CacheStatus.class).readValue(json);
		} catch (IOException e) {
			log.error("Error during deserialization ", e);
			throw new RuntimeException(e);
		}
	}

	public <O> O getCacheEntry(String entityId, CacheAlliance cacheSpace, Class<O> clazz) {

		String json = getCacheSpace(cacheSpace).get(entityId);
		if (json == null)
			return null;

		try {
			return mapper.readValue(json, clazz);
		} catch (IOException e) {
			log.error("Error during deserialization ", e);
			throw new RuntimeException(e);
		}
	}

	public <O> String getCacheEntryString(String entityId, CacheAlliance cacheSpace, Class<O> clazz) {
		return getCacheSpace(cacheSpace).get(entityId);
	}

	public void putCacheEntry(String primaryKey, List<?> items, Class<?> classView, CacheAlliance cacheAlliance) {
		RemoteCache<String, String> cache = getCacheSpace(cacheAlliance);
		String value;
		try {
			value = mapper.writerWithView(classView).writeValueAsString(items);
			cache.put(primaryKey, value);
		} catch (JsonProcessingException e) {
			log.error("error while saving entry into cache", e);
			throw new RuntimeException(e);
		}
	}

	public void putCacheEntry(String primaryKey, Object object, Class<?> classView, CacheAlliance cacheAlliance) {
		RemoteCache<String, String> cache = getCacheSpace(cacheAlliance);
		String value = null;
		try {
			value = mapper.writerWithView(classView).writeValueAsString(object);
			cache.put(primaryKey, value);
		} catch (JsonProcessingException e) {
			log.error("error while saving entry into cache", e);
			throw new RuntimeException(e);
		} catch (Exception e ) {
			try {
				mapper.writerWithView(classView).writeValue(new File("/data/error.json"), object);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			log.info(primaryKey);
			log.info(value.length() + "");
			log.info(classView + "");
			log.info(cacheAlliance + "");
			log.error(e + "");
			throw new RuntimeException(e);
		}
	}

}
