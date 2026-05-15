package org.alliancegenome.api.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import org.alliancegenome.view.PublicView;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RibbonEntity {

	@JsonView({ PublicView.DiseaseAnnotation.class, PublicView.Expression.class }) private String id;
	@JsonView({ PublicView.DiseaseAnnotation.class, PublicView.Expression.class }) private String label;
	@JsonView({ PublicView.DiseaseAnnotation.class, PublicView.Expression.class })
	@JsonProperty("taxon_id") private String taxonID;
	@JsonView({ PublicView.DiseaseAnnotation.class, PublicView.Expression.class })
	@JsonProperty("taxon_label") private String taxonName;
	@JsonView({ PublicView.DiseaseAnnotation.class, PublicView.Expression.class })
	@JsonProperty("nb_classes") private int numberOfClasses;
	@JsonView({ PublicView.DiseaseAnnotation.class, PublicView.Expression.class })
	@JsonProperty("nb_annotations") private int numberOfAnnotations;

	@JsonView({ PublicView.DiseaseAnnotation.class, PublicView.Expression.class })
	@JsonProperty("groups")
	// <disease ID, EntitySubgroupSlim
	private Map<String, Map<String, Object>> slims = new LinkedHashMap<>();

	public void addEntitySlim(EntitySubgroupSlim slim) {
		String id = slim.getId();
		if (id == null) {
			id = "nullID";
		}
		Map<String, Object> subgroupSlimMap = new LinkedHashMap<>();
		subgroupSlimMap.put("ALL", slim);
		if (slim.getAvailable() != null) {
			subgroupSlimMap.put("available", slim.getAvailable().toString());
		}
		slims.put(id, subgroupSlimMap);
	}
}
