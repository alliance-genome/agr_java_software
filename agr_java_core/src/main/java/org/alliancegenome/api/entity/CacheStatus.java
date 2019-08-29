package org.alliancegenome.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class CacheStatus implements Serializable {

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
