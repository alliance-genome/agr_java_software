package org.alliancegenome.api.service.helper;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class ExpressionSummaryGroup {
    @JsonView({View.ExpressionView.class})
    private String name;
    @JsonView({View.ExpressionView.class})
    private long totalAnnotations;
    @JsonView({View.ExpressionView.class})
    private List<ExpressionSummaryGroupTerm> terms;

    public void addGroupTerm(ExpressionSummaryGroupTerm term) {
        if (terms == null)
            terms = new ArrayList<>();
        terms.add(term);
    }

    @Override
    public String toString() {
        return name + " [" + totalAnnotations + ']';
    }
}

