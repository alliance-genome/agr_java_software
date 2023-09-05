package org.alliancegenome.neo4j.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(name="OrthologView", description="POJO that represents the Paralog view")
@JsonPropertyOrder({"gene", "homologGene", "length", "similarity", "identity", "rank", "predictionMethodsMatched", "predictionMethodsNotMatched", "predictionMethodsNotCalled", "methodCount", "totalMethodCount"})
public class ParalogBean implements Serializable {

	@JsonView(View.Homology.class)
	private Gene gene;
	@JsonView(View.Homology.class)
	private Gene homologGene;

	@JsonView(View.Homology.class)
	private String length;
	@JsonView(View.Homology.class)
	private String similarity;
	@JsonView(View.Homology.class)
	private Integer rank;
	@JsonView(View.Homology.class)
	private String identity;

	@JsonView(View.Homology.class)
	private List<String> predictionMethodsNotCalled;
	@JsonView(View.Homology.class)
	private List<String> predictionMethodsMatched;
	@JsonView(View.Homology.class)
	private List<String> predictionMethodsNotMatched;

	@JsonView(View.Homology.class)
	@JsonProperty(value = "methodCount")
	public Integer getMethodCount() {
		if (predictionMethodsMatched == null)
			return 0;
		return predictionMethodsMatched.size();
	}

	@JsonProperty(value = "methodCount")
	public void setMethodCount(Integer count) {
	}

	@JsonView(View.Homology.class)
	@JsonProperty(value = "totalMethodCount")
	public Integer getTotalMethodCount() {
		if (predictionMethodsMatched == null && predictionMethodsNotMatched == null)
			return 0;
		if (predictionMethodsMatched == null)
			return predictionMethodsNotMatched.size();
		if (predictionMethodsNotMatched == null)
			return predictionMethodsMatched.size();
		return predictionMethodsMatched.size() + predictionMethodsNotMatched.size();
	}

	@JsonProperty(value = "totalMethodCount")
	public void setTotalMethodCount(Integer count) {
	}

}
