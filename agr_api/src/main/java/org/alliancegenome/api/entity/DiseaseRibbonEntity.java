package org.alliancegenome.api.entity;

import java.util.LinkedHashMap;
import java.util.Map;

import org.alliancegenome.core.view.PublicView;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiseaseRibbonEntity {

	@JsonView(PublicView.DiseaseAnnotation.class)
	private String id;
	@JsonView(PublicView.DiseaseAnnotation.class)
	private String label;
	@JsonView(PublicView.DiseaseAnnotation.class)
	@JsonProperty("taxon_id")
	private String taxonID;
	@JsonView(PublicView.DiseaseAnnotation.class)
	@JsonProperty("taxon_label")
	private String taxonName;
	@JsonView(PublicView.DiseaseAnnotation.class)
	@JsonProperty("nb_classes")
	private int numberOfClasses;
	@JsonView(PublicView.DiseaseAnnotation.class)
	@JsonProperty("nb_annotations")
	private int numberOfAnnotations;

	@JsonView(PublicView.DiseaseAnnotation.class)
	@JsonProperty("groups")
	// <disease ID, DiseaseEntitySubgroupSlim
	private Map<String, Map<String, DiseaseEntitySubgroupSlim>> slims = new LinkedHashMap<>();

	public void addDiseaseSlim(DiseaseEntitySubgroupSlim slim) {
		String id = slim.getId();
		if (id == null) {
			id = "nullID";
		}
		Map<String, DiseaseEntitySubgroupSlim> subgroupSlimMap = new LinkedHashMap<>();
		subgroupSlimMap.put("ALL", slim);
		slims.put(id, subgroupSlimMap);
	}
}