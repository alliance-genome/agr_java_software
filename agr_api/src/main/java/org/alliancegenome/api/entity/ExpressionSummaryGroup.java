package org.alliancegenome.api.entity;

import java.util.*;

import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.*;

@Setter
@Getter
public class ExpressionSummaryGroup {
    @JsonView({View.Expression.class})
    private String name;
    @JsonView({View.Expression.class})
    private long totalAnnotations;
    private long totalClasses;
    @JsonView({View.Expression.class})
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

