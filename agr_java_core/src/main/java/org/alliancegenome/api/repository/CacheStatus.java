package org.alliancegenome.api.repository;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.core.service.Duration;

import java.time.LocalDateTime;

@Getter
@Setter
public class CacheStatus {

    private String name;
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

}
