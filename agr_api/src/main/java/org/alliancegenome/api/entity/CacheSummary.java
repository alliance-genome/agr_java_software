package org.alliancegenome.api.entity;

import java.util.*;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

@Getter
@Setter
public class CacheSummary {

    @JsonView(View.Cacher.class)
    List<CacheStatus> cacheStatusList = new ArrayList<>();

    public void addCacheStatus(CacheStatus status) {
        cacheStatusList.add(status);
    }
}
