package org.alliancegenome.api.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.api.rest.interfaces.CacheRESTInterface;
import org.alliancegenome.api.service.CacheStatusService;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Path("/cache")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CacheController implements CacheRESTInterface {

	@Inject CacheStatusService service;

	@Override
	public JsonResultResponse<CacheStatus> getCacheStatus(
			int limit,
			int page,
			String sortBy,
			String asc,
			String indexName
	) {
		long startTime = System.currentTimeMillis();
		Pagination pagination = new Pagination(page, limit, sortBy, asc);
		pagination.addFieldFilter(FieldFilter.INDEX_NAME, indexName);
		JsonResultResponse<CacheStatus> summary = new JsonResultResponse<>();
		Map<CacheAlliance, CacheStatus> map = service.getAllCachStatusRecords();

		List<CacheStatus> results = new ArrayList<>(map.values());

		List<CacheStatus> paginatedResults = results.stream()
				.skip(pagination.getStart())
				.limit(pagination.getLimit())
				.collect(Collectors.toList());

		summary.setResults(paginatedResults);
		summary.setTotal(map.values().size());
		summary.calculateRequestDuration(startTime);
		return summary;
	}

	@Override
	public CacheStatus getCacheStatusPerSpace(String cacheSpace) {
		return service.getCacheStatus(CacheAlliance.getTypeByName(cacheSpace));
	}

	@Override
	public String getCacheEntryString(String entityId, String cacheName) {
		return service.getCacheEntryString(entityId, cacheName);
	}

}
