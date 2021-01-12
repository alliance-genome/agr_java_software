package org.alliancegenome.api.entity;

import java.io.Serializable;
import java.util.*;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.neo4j.view.View;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.*;

import lombok.*;

@Getter
@Setter
@ToString
@Schema(name="CacheStatus", description="POJO that represents Cache status")
@JsonPropertyOrder({"name", "entitiesInCache", "speciesStats", "entityStats"})
public class CacheStatus implements Serializable {

    @JsonView(View.Cacher.class)
    private String name;
    @JsonView(View.Cacher.class)
    private int numberOfEntityIDs;
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
    @JsonView(View.Cacher.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private Date dateCreated = new Date();
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
