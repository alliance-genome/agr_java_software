package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter @ToString
public class CacheStatus {

    private String name;
    @JsonIgnore
    private int numberOfEntities;

    public CacheStatus(String name) {
        this.name = name;
    }

    @JsonProperty("entitiesInCache")
    public String getEntitiesInCache() {
        return String.format("%,d", numberOfEntities);
    }

}
