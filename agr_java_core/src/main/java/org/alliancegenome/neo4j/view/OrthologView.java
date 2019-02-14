package org.alliancegenome.neo4j.view;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.Setter;
import org.alliancegenome.neo4j.entity.node.Gene;

import java.util.List;
import java.util.Objects;

@Setter
@Getter
public class OrthologView {

    @JsonView(View.Orthology.class)
    private Gene gene;
    @JsonView(View.Orthology.class)
    private Gene homologGene;

    @JsonView(View.Orthology.class)
    private boolean best;
    @JsonView(View.Orthology.class)
    private boolean bestReverse;
    @JsonView(View.Orthology.class)
    private String stringencyFilter;

    @JsonView(View.Orthology.class)
    private List<String> predictionMethodsNotCalled;
    @JsonView(View.Orthology.class)
    private List<String> predictionMethodsMatched;
    @JsonView(View.Orthology.class)
    private List<String> predictionMethodsNotMatched;

    @JsonView(View.Orthology.class)
    private Integer methodCount;
    @JsonView(View.Orthology.class)
    private Integer totalMethodCount;

    public void calculateCounts() {
        methodCount = predictionMethodsMatched.size();
        totalMethodCount = predictionMethodsMatched.size() + predictionMethodsNotMatched.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrthologView that = (OrthologView) o;
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
