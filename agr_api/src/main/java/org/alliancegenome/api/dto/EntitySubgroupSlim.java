package org.alliancegenome.api.dto;

import org.alliancegenome.core.view.PublicView;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EntitySubgroupSlim {

	private String id;
	@JsonView({ PublicView.DiseaseAnnotation.class, PublicView.Expression.class })
	@JsonProperty("nb_classes") private int numberOfClasses;
	@JsonView({ PublicView.DiseaseAnnotation.class, PublicView.Expression.class })
	@JsonProperty("nb_annotations") private int numberOfAnnotations;

	private Boolean available;

	public void setAvailable(Boolean available) {
		// only set this variable if it is false.
		// if it is true it is covered by the default behavior in the ribbon code
		if (available != null && !available) {
			this.available = available;
		}
	}
}
