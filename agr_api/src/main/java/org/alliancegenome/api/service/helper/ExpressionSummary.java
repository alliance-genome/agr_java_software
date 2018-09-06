package org.alliancegenome.api.service.helper;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ExpressionSummary {

    @JsonView({ View.ExpressionView.class})
    private int totalAnnotations;
    @JsonView({ View.ExpressionView.class})
    private List<ExpressionSummaryGroup> groups = new ArrayList<>();

    public void addGroup(ExpressionSummaryGroup group){
        groups.add(group);
    }

}
