package org.alliancegenome.core.service;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class JsonResultResponse<T> {

    @JsonView(View.OrthologyView.class)
    private List<T> results;
    @JsonView(View.OrthologyView.class)
    private int total;
    @JsonView(View.OrthologyView.class)
    private int returnedRecords;
    @JsonView(View.OrthologyView.class)
    private String errorMessage = "";
    @JsonView(View.OrthologyView.class)
    private String requestDuration;
    @JsonView(View.OrthologyView.class)
    private String apiVersion;

    public void setRequestDuration(LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = new Duration(startTime, endTime);
        requestDuration = duration.toString();
    }

    public void setResults(List<T> results) {
        this.results = results;
        if (results != null)
            returnedRecords = results.size();
    }
}
