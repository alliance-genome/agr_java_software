package org.alliancegenome.api.dto;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CacheSummary {

	@JsonView(View.Cacher.class)
	List<CacheStatus> cacheStatusList = new ArrayList<>();

	public void addCacheStatus(CacheStatus status) {
		cacheStatusList.add(status);
	}
}
