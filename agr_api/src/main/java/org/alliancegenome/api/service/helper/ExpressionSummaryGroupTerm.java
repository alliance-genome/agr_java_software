package org.alliancegenome.api.service.helper;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExpressionSummaryGroupTerm {
    @JsonView({ View.ExpressionAPI.class})
    private String id;
    @JsonView({ View.ExpressionAPI.class})
    private String name;
    @JsonView({ View.ExpressionAPI.class})
    private int numberOfAnnotations;

    @Override
    public String toString() {
        return name + " [" + numberOfAnnotations + ']';
    }
}

