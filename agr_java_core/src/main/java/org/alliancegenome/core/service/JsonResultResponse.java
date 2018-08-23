package org.alliancegenome.core.service;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

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
    private String errorMessage;

}
