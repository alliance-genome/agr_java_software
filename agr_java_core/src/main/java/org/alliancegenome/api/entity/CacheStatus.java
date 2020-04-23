package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.neo4j.view.View;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@ToString
@JsonPropertyOrder({"name", "entitiesInCache", "speciesStats", "entityStats"})
public class CacheStatus implements Serializable {

    @JsonView(View.Cacher.class)
    private String name;
    @JsonIgnore
    private int numberOfEntities;
    @JsonView(View.CacherDetail.class)
    Map<String, Integer> entityStats;
    @JsonView(View.Cacher.class)
    Map<String, Integer> speciesStats;
    @JsonView(View.Cacher.class)
    private String entitiesInCache;
    @JsonView(View.Cacher.class)
    private String collectionEntity;
    @JsonView(View.Cacher.class)
    private String jsonViewClass;
    @JsonIgnore
    private CacheAlliance cache;

    public CacheStatus(CacheAlliance cache) {
        this.cache = cache;
        this.name = cache.getCacheName();
    }

    public CacheStatus() {
    }

    public void setNumberOfEntities(int numberOfEntities) {
        this.numberOfEntities = numberOfEntities;
        this.entitiesInCache = String.format("%,d", numberOfEntities);
    }
}
