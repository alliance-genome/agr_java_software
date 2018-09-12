package org.alliancegenome.api.service.helper;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.util.List;

@Setter
@Getter
public class ExpressionSummaryGroupTerm {
    @JsonView({ View.ExpressionView.class})
    private String id;
    @JsonView({ View.ExpressionView.class})
    private String name;
    @JsonView({ View.ExpressionView.class})
    private int numberOfAnnotations;
}

