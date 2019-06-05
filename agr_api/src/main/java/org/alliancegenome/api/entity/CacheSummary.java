package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Getter
@Setter
public class CacheSummary {

    List<CacheStatus> cacheStatusList = new ArrayList<>();

    public void addCacheStatus(CacheStatus status) {
        cacheStatusList.add(status);
    }

    @JsonProperty("stillCaching")
    public boolean stillCaching() {
        Set<Boolean> caching = cacheStatusList.stream()
                .map(CacheStatus::isCaching)
                .collect(toSet());
        return caching.contains(true);
    }
}
