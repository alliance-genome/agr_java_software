package org.alliancegenome.api.repository;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class CacheStatus {

    private Map<String, Boolean> cachingMap = new HashMap<>();

    public void addCacheInfo(String name, boolean isCaching) {
        cachingMap.put(name, isCaching);
    }
}
