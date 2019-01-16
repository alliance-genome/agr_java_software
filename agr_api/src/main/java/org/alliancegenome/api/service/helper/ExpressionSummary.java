package org.alliancegenome.api.service.helper;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExpressionSummary {

    @JsonView({ View.Expression.class})
    private int totalAnnotations;
    @JsonView({ View.Expression.class})
    private List<ExpressionSummaryGroup> groups = new ArrayList<>();

    public void addGroup(ExpressionSummaryGroup group){
        groups.add(group);
    }

}
