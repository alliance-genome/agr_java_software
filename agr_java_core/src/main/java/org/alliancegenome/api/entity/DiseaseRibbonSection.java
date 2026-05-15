package org.alliancegenome.api.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.view.PublicView;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiseaseRibbonSection implements Serializable {

	@JsonView({PublicView.DiseaseAnnotation.class})
	private String id;
	@JsonView({PublicView.DiseaseAnnotation.class})
	private String label;
	@JsonView({PublicView.DiseaseAnnotation.class})
	private String description;
	@JsonProperty("class_label")
	private String classLabel;
	@JsonProperty("annotation_label")
	private String annotationLabel;

	@JsonView({PublicView.DiseaseAnnotation.class})
	@JsonProperty("groups")
	private List<SectionSlim> slims = new ArrayList<>();

	public void addDiseaseSlim(SectionSlim slim) {
		slims.add(slim);
	}
}
