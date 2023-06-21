package org.alliancegenome.neo4j.view;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.alliancegenome.neo4j.entity.node.Gene;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(name="OrthologView", description="POJO that represents the Ortholog view")
@JsonPropertyOrder({"gene", "homologGene", "best", "bestReverse", "stringencyFilter", "predictionMethodsMatched", "predictionMethodsNotMatched", "predictionMethodsNotCalled", "methodCount", "totalMethodCount"})
public class HomologView implements Serializable {

	@JsonView(View.Homology.class)
	private Gene gene;
	@JsonView(View.Homology.class)
	private Gene homologGene;

	@JsonView(View.Homology.class)
	private String best;
	@JsonView(View.Homology.class)
	private String bestReverse;
	@JsonView(View.Homology.class)
	private String stringencyFilter = "all";

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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		HomologView that = (HomologView) o;
		return best == that.best &&
				bestReverse == that.bestReverse &&
				Objects.equals(gene, that.gene) &&
				Objects.equals(homologGene, that.homologGene);
	}

	@Override
	public int hashCode() {

		return Objects.hash(gene, homologGene, best, bestReverse);
	}

}
