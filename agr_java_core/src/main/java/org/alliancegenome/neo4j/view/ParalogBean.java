package org.alliancegenome.neo4j.view;

import java.io.Serializable;
import java.util.List;

import org.alliancegenome.neo4j.entity.node.Gene;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Data;

@Data
@Schema(name = "OrthologView", description = "POJO that represents the Paralog view")
@JsonPropertyOrder({"gene", "homologGene", "length", "similarity", "identity", "rank", "predictionMethodsMatched", "predictionMethodsNotMatched", "predictionMethodsNotCalled", "methodCount", "totalMethodCount"})
public class ParalogBean implements Serializable {

	@JsonView(PublicView.Homology.class)
	private Gene gene;
	@JsonView(PublicView.Homology.class)
	private Gene homologGene;

	@JsonView(PublicView.Homology.class)
	private String length;
	@JsonView(PublicView.Homology.class)
	private String similarity;
	@JsonView(PublicView.Homology.class)
	private String rank;
	@JsonView(PublicView.Homology.class)
	private String identity;

	@JsonView(PublicView.Homology.class)
	private List<String> predictionMethodsNotCalled;
	@JsonView(PublicView.Homology.class)
	private List<String> predictionMethodsMatched;
	@JsonView(PublicView.Homology.class)
	private List<String> predictionMethodsNotMatched;

	@JsonView(PublicView.Homology.class)
	@JsonProperty(value = "methodCount")
	public Integer getMethodCount() {
		if (predictionMethodsMatched == null) {
			return 0;
		}
		return predictionMethodsMatched.size();
	}

	@JsonProperty(value = "methodCount")
	public void setMethodCount(Integer count) {
	}

	@JsonView(PublicView.Homology.class)
	@JsonProperty(value = "totalMethodCount")
	public Integer getTotalMethodCount() {
		if (predictionMethodsMatched == null && predictionMethodsNotMatched == null) {
			return 0;
		}
		if (predictionMethodsMatched == null) {
			return predictionMethodsNotMatched.size();
		}
		if (predictionMethodsNotMatched == null) {
			return predictionMethodsMatched.size();
		}
		return predictionMethodsMatched.size() + predictionMethodsNotMatched.size();
	}

	@JsonProperty(value = "totalMethodCount")
	public void setTotalMethodCount(Integer count) {
	}

}
