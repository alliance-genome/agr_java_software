package org.alliancegenome.neo4j.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Setter
@Getter
@Schema(name="ParalogView", description="POJO that represents the Paralog view")
@JsonPropertyOrder({"gene", "paralogGene", "best", "bestReverse", "stringencyFilter", "predictionMethodsMatched", "predictionMethodsNotMatched", "predictionMethodsNotCalled", "methodCount", "totalMethodCount"})
public class ParalogView implements Serializable {

	@JsonView(View.Orthology.class)
	private Gene gene;
	@JsonView(View.Orthology.class)
	private Gene homologGene;

	@JsonView(View.Orthology.class)
	private String best;
	@JsonView(View.Orthology.class)
	private String bestReverse;
	@JsonView(View.Orthology.class)
	private String stringencyFilter = "all";

	@JsonView(View.Orthology.class)
	private List<String> predictionMethodsNotCalled;
	@JsonView(View.Orthology.class)
	private List<String> predictionMethodsMatched;
	@JsonView(View.Orthology.class)
	private List<String> predictionMethodsNotMatched;

	@JsonView(View.Orthology.class)
	@JsonProperty(value = "methodCount")
	public Integer getMethodCount() {
		if (predictionMethodsMatched == null)
			return 0;
		return predictionMethodsMatched.size();
	}

	@JsonProperty(value = "methodCount")
	public void setMethodCount(Integer count) {
	}

	@JsonView(View.Orthology.class)
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
		ParalogView that = (ParalogView) o;
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
