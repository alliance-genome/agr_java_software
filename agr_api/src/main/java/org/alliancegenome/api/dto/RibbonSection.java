package org.alliancegenome.api.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.api.entity.SectionSlim;
import org.alliancegenome.view.PublicView;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RibbonSection implements Serializable {

	@JsonView({ PublicView.DiseaseAnnotation.class, PublicView.Expression.class }) private String id;
	@JsonView({ PublicView.DiseaseAnnotation.class, PublicView.Expression.class }) private String label;
	@JsonView({ PublicView.DiseaseAnnotation.class, PublicView.Expression.class }) private String description;
	@JsonProperty("class_label") private String classLabel;
	@JsonProperty("annotation_label") private String annotationLabel;

	@JsonView({ PublicView.DiseaseAnnotation.class, PublicView.Expression.class })
	@JsonProperty("groups") private List<SectionSlim> slims = new ArrayList<>();

	public void addDiseaseSlim(SectionSlim slim) {
		slims.add(slim);
	}
}
