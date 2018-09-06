package org.alliancegenome.neo4j.view;

import java.util.List;
import java.util.Objects;

import org.alliancegenome.neo4j.entity.node.Gene;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrthologView  {

    @JsonView(View.OrthologyView.class)
    private Gene gene;
    @JsonView(View.OrthologyView.class)
    private Gene homologGene;

    @JsonView(View.OrthologyView.class)
    private boolean best;
    @JsonView(View.OrthologyView.class)
    private boolean bestReverse;

    @JsonView(View.OrthologyView.class)
    private List<String> predictionMethodsNotCalled;
    @JsonView(View.OrthologyView.class)
    private List<String> predictionMethodsMatched;
    @JsonView(View.OrthologyView.class)
    private List<String> predictionMethodsNotMatched;

    @JsonView(View.OrthologyView.class)
    private Integer methodCount;
    @JsonView(View.OrthologyView.class)
    private Integer totalMethodCount;

    public void calculateCounts() {
        methodCount=predictionMethodsMatched.size();
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
