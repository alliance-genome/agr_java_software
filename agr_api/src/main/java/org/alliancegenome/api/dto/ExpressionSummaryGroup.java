package org.alliancegenome.api.dto;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.neo4j.view.PublicView;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExpressionSummaryGroup {
	@JsonView({ PublicView.Expression.class }) private String name;
	@JsonView({ PublicView.Expression.class }) private long totalAnnotations;
	private long totalClasses;
	@JsonView({ PublicView.Expression.class }) private List<ExpressionSummaryGroupTerm> terms;

	public void addGroupTerm(ExpressionSummaryGroupTerm term) {
		if (terms == null) {
			terms = new ArrayList<>();
		}
		terms.add(term);
	}

	@Override
	public String toString() {
		return name + " [" + totalAnnotations + ']';
	}
}
