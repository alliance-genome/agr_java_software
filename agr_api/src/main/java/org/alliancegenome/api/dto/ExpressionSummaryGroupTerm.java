package org.alliancegenome.api.dto;

import org.alliancegenome.neo4j.view.PublicView;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExpressionSummaryGroupTerm {
	@JsonView({ PublicView.Expression.class})
	private String id;
	@JsonView({ PublicView.Expression.class})
	private String name;
	@JsonView({ PublicView.Expression.class})
	private int numberOfAnnotations;
	private int numberOfClasses;

	@Override
	public String toString() {
		return name + " [" + numberOfAnnotations + ']';
	}
}

