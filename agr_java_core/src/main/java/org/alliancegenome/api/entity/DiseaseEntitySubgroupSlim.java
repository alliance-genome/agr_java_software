package org.alliancegenome.api.entity;

import org.alliancegenome.neo4j.view.PublicView;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DiseaseEntitySubgroupSlim {

	private String id;
	@JsonView(PublicView.DiseaseAnnotation.class)
	@JsonProperty("nb_classes")
	private long numberOfClasses;
	@JsonView(PublicView.DiseaseAnnotation.class)
	@JsonProperty("nb_annotations")
	private long numberOfAnnotations;

}
