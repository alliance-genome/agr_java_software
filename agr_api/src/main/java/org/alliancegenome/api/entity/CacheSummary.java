package org.alliancegenome.api.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

@Getter
@Setter
public class CacheSummary {

    @JsonView(View.Cacher.class)
    List<CacheStatus> cacheStatusList = new ArrayList<>();

    public void addCacheStatus(CacheStatus status) {
        cacheStatusList.add(status);
    }
}
