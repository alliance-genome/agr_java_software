package org.alliancegenome.api.entity;

import java.time.LocalDateTime;

import org.alliancegenome.core.service.Duration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CacheStatus {

    private String name;
    @JsonIgnore
    private int numberOfEntities;
    private boolean caching;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime start;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime end;

    public CacheStatus(String name) {
        this.name = name;
    }

    public String getDuration() {
        if (start == null)
            return "Not yet started";
        if (end == null)
            end = LocalDateTime.now();
        Duration duration = new Duration(start, end);
        return duration.toString();
    }

    @JsonProperty("entitiesInCache")
    public String getEntitiesInCache() {
        return String.format("%,d", numberOfEntities);
    }

}
