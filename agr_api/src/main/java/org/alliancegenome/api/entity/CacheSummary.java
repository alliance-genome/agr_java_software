package org.alliancegenome.api.entity;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CacheSummary {

    List<CacheStatus> cacheStatusList = new ArrayList<>();

    public void addCacheStatus(CacheStatus status) {
        cacheStatusList.add(status);
    }
}
